package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.distributionQueries
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.config.VersionHistoryConfig
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.{DatabaseCollections, DeveloperDesiredVersionsDocument, DeveloperVersionInfoDocument}
import org.bson.BsonDocument
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DeveloperVersionUtils extends DistributionClientsUtils with StateUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val versionHistoryConfig: VersionHistoryConfig

  protected implicit val executionContext: ExecutionContext

  def addDeveloperVersionInfo(versionInfo: DeveloperVersionInfo): Future[Boolean] = {
    log.info(s"Add developer version info ${versionInfo}")
    (for {
      collection <- collections.Developer_VersionsInfo
      id <- collections.getNextSequence(collection.getName())
      info <- {
        val document = DeveloperVersionInfoDocument(id, versionInfo)
        collection.insert(document).map(_ => document)
      }.map(_.info)
      _ <- removeObsoleteVersions(info.version.distributionName, info.serviceName)
    } yield info).map(_ => true)
  }

  private def removeObsoleteVersions(distributionName: DistributionName, serviceName: ServiceName): Future[Unit] = {
    for {
      versions <- getDeveloperVersionsInfo(serviceName, distributionName = Some(distributionName))
      busyVersions <- getBusyVersions(distributionName, serviceName)
      complete <- {
        val notUsedVersions = versions.filterNot(info => busyVersions.contains(info.version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > versionHistoryConfig.maxSize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - versionHistoryConfig.maxSize).map { version =>
            removeDeveloperVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield {}
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion): Future[Boolean] = {
    log.info(s"Remove developer version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("info.serviceName", serviceName),
      Filters.eq("info.version", version))
    dir.getDeveloperVersionImageFile(serviceName, version).delete()
    for {
      collection <- collections.Developer_VersionsInfo
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  def getDeveloperVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None,
                               version: Option[DeveloperDistributionVersion] = None): Future[Seq[DeveloperVersionInfo]] = {
    val serviceArg = Filters.eq("info.serviceName", serviceName)
    val distributionArg = distributionName.map { distributionName => Filters.eq("info.version.distributionName", distributionName ) }
    val versionArg = version.map { version => Filters.eq("info.version", version) }
    val filters = Filters.and((Seq(serviceArg) ++ distributionArg ++ versionArg).asJava)
    for {
      collection <- collections.Developer_VersionsInfo
      info <- collection.find(filters).map(_.map(_.info))
    } yield info
  }

  def setDeveloperDesiredVersions(desiredVersions: Seq[DeveloperDesiredVersion]): Future[Boolean] = {
    log.info(s"Set developer desired versions ${desiredVersions}")
    for {
      collection <- collections.Developer_DesiredVersions
      result <- collection.replace(new BsonDocument(), DeveloperDesiredVersionsDocument(desiredVersions)).map(_ => true)
    } yield result
  }

  def getDeveloperDesiredVersions(serviceNames: Set[ServiceName]): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      collection <- collections.Developer_DesiredVersions
      profile <- collection.find(new BsonDocument()).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DeveloperDesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def getDeveloperDesiredVersion(serviceName: ServiceName): Future[Option[DeveloperDistributionVersion]] = {
    getDeveloperDesiredVersions(Set(serviceName)).map(_.headOption.map(_.version))
  }

  def filterDesiredVersionsByProfile(distributionName: DistributionName, future: Future[Seq[DeveloperDesiredVersion]]): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getDistributionClientInstallProfile(distributionName)
      versions <- Future(desiredVersions.filter(version => installProfile.profile.services.contains(version.serviceName)))
    } yield versions
  }

  def getDeveloperDesiredVersions(distributionName: DistributionName, serviceNames: Set[ServiceName]): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      distributionClientConfig <- getDistributionClientConfig(distributionName)
      developerVersions <- distributionClientConfig.testDistributionMatch match {
        case Some(testDistributionMatch) =>
          for {
            testedVersions <- getTestedVersions(distributionClientConfig.installProfile).map(testedVersions => {
              testedVersions match {
                case Some(testedVersions) =>
                  val regexp = testDistributionMatch.r
                  val testCondition = testedVersions.signatures.exists(signature =>
                    signature.distributionName match {
                      case regexp() =>
                        true
                      case _ =>
                        false
                    })
                  if (testCondition) {
                    testedVersions.versions
                  } else {
                    throw NotFoundException(s"Desired versions for profile ${distributionClientConfig.installProfile} are not tested by clients ${testDistributionMatch}")
                  }
                case None =>
                  throw NotFoundException(s"Desired versions for profile ${distributionClientConfig.installProfile} are not tested by anyone")
              }
            })
          } yield testedVersions
        case None =>
          getDeveloperDesiredVersions(serviceNames)
      }
    } yield developerVersions
  }

  def downloadUpdates(developerDistributionClient: DistributionClient, serviceNames: Seq[ServiceName])(implicit log: Logger):
        Future[Map[ServiceName, DeveloperDistributionVersion]] = {
    for {
      developerDesiredVersions <- developerDistributionClient.graphqlRequest(distributionQueries.getDesiredVersions(serviceNames))
          .map(DeveloperDesiredVersions.toMap(_))
      updatedVersions <- (Future.sequence(developerDesiredVersions.map {
        case (serviceName, version) if version.distributionName == developerDistributionClient.distributionName =>
          for {
            existVersionInfo <- getDeveloperVersionsInfo(serviceName, Some(developerDistributionClient.distributionName), Some(version)).map(_.headOption)
            updatedVersion <- existVersionInfo match {
              case Some(_) =>
                Future(None)
              case None =>
                for {
                  _ <- {
                    log.info(s"Download version ${version}")
                    val imageFile = File.createTempFile("version", "image")
                    developerDistributionClient.downloadDeveloperVersionImage(serviceName, version, imageFile)
                      .andThen {
                        case Success(_) =>
                          imageFile.renameTo(dir.getDeveloperVersionImageFile(serviceName, version))
                        case _ =>
                      }.andThen { case _ => imageFile.delete() }
                  }
                  versionInfo <- developerDistributionClient.graphqlRequest(distributionQueries.getVersionsInfo(serviceName, None, Some(version))).map(_.headOption)
                  updatedVersion <- versionInfo match {
                    case Some(versionInfo) =>
                      addDeveloperVersionInfo(versionInfo).map(result => if (result) Some(serviceName -> version) else None)
                    case None =>
                      Future(None)
                  }
                } yield updatedVersion
            }
          } yield updatedVersion
        case _ =>
          Future(None)
      }).map(_.flatten.toMap))
    } yield updatedVersions
  }

  private def getBusyVersions(distributionName: DistributionName, serviceName: ServiceName): Future[Set[DeveloperVersion]] = {
    for {
      desiredVersion <- getDeveloperDesiredVersion(serviceName)
      clientsInfo <- getDistributionClientsInfo()
      installedVersions <- Future.sequence(clientsInfo.map(client => getInstalledDesiredVersion(client.distributionName, serviceName))).map(
        _.flatten.map(_.version.original()))
      testedVersions <- Future.sequence(clientsInfo.map(client => getTestedVersions(client.clientConfig.installProfile))).map(
        _.flatten.map(_.versions.find(_.serviceName == serviceName).map(_.version)).flatten)
    } yield {
      (desiredVersion.toSet ++ installedVersions ++ testedVersions).filter(_.distributionName == distributionName).map(_.version)
    }
  }
}