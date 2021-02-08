package com.vyulabs.update.distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName, TaskId, UserName}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.administratorQueries
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.distributionQueries
import com.vyulabs.update.common.distribution.client.{DistributionClient, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.graphql.NotFoundException
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.bson.BsonDocument
import org.slf4j.Logger

import java.io.File
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

trait DeveloperVersionUtils extends DistributionClientsUtils with StateUtils with RunBuilderUtils with SprayJsonSupport {
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val config: DistributionConfig
  protected val taskManager: TaskManager

  protected implicit val executionContext: ExecutionContext

  def buildDeveloperVersion(serviceName: ServiceName, developerVersion: DeveloperVersion, author: UserName,
                            sourceBranches: Seq[String], comment: Option[String])(implicit log: Logger): TaskId = {
    val task = taskManager.create(s"Build developer version ${developerVersion} of service ${serviceName}",
      (taskId, logger) => {
        implicit val log = logger
        val arguments = Seq("buildDeveloperVersion",
          s"distributionName=${config.distributionName}", s"service=${serviceName}", s"version=${developerVersion.toString}", s"author=${author}",
          s"sourceBranches=${sourceBranches.foldLeft("")((branches, branch) => { branches + (if (branches.isEmpty) branch else s",${branch}}") })}") ++
          comment.map(comment => s"comment=${comment}")
        runBuilder(taskId, arguments)
      })
    task.taskId
  }

  def generateNewVersionNumber(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName)(implicit log: Logger): DeveloperDistributionVersion = {
    log.info("Get existing versions")
    distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName, Some(distributionClient.distributionName))) match {
      case Some(versions) if !versions.isEmpty =>
        val lastVersion = versions.map(_.version).sorted(DeveloperDistributionVersion.ordering).last
        log.info(s"Last version is ${lastVersion}")
        lastVersion.next()
      case _ =>
        log.error("No existing versions")
        DeveloperDistributionVersion(distributionClient.distributionName, DeveloperVersion(Seq(1, 0, 0)))
    }
  }

  def addDeveloperVersionInfo(versionInfo: DeveloperVersionInfo)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Add developer version info ${versionInfo}")
    (for {
      _ <- collections.Developer_VersionsInfo.insert(versionInfo)
      _ <- removeObsoleteVersions(versionInfo.version.distributionName, versionInfo.serviceName)
    } yield {}).map(l => true)
  }

  private def removeObsoleteVersions(distributionName: DistributionName, serviceName: ServiceName)(implicit log: Logger): Future[Unit] = {
    for {
      versions <- getDeveloperVersionsInfo(serviceName, distributionName = Some(distributionName))
      busyVersions <- getBusyVersions(distributionName, serviceName)
      complete <- {
        val notUsedVersions = versions.filterNot(info => busyVersions.contains(info.version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > config.versionHistory.maxSize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - config.versionHistory.maxSize).map { version =>
            removeDeveloperVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield {}
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion)(implicit log: Logger): Future[Boolean] = {
    log.info(s"Remove developer version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("serviceName", serviceName),
      Filters.eq("version", version))
    dir.getDeveloperVersionImageFile(serviceName, version).delete()
    for {
      profile <- {
        collections.Developer_VersionsInfo.delete(filters).map(_ > 0)
      }
    } yield profile
  }

  def getDeveloperVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None,
                               version: Option[DeveloperDistributionVersion] = None)(implicit log: Logger): Future[Seq[DeveloperVersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val distributionArg = distributionName.map { distributionName => Filters.eq("version.distributionName", distributionName ) }
    val versionArg = version.map { version => Filters.eq("version", version) }
    val filters = Filters.and((Seq(serviceArg) ++ distributionArg ++ versionArg).asJava)
    collections.Developer_VersionsInfo.find(filters)
  }

  def setDeveloperDesiredVersions(servicesVersions: Map[ServiceName, Option[DeveloperDistributionVersion]])
                                 (implicit log: Logger): Future[Unit] = {
    log.info(s"Upload developer desired versions ${servicesVersions}")
    for {
      desiredVersions <- getDeveloperDesiredVersions(servicesVersions.keySet)
      result <- {
        var desiredVersionsMap = DeveloperDesiredVersions.toMap(desiredVersions)
        servicesVersions.foreach {
          case (serviceName, Some(version)) =>
            desiredVersionsMap += (serviceName -> version)
          case (serviceName, None) =>
            desiredVersionsMap -= serviceName
        }
        val desiredVersionsList = desiredVersionsMap.foldLeft(Seq.empty[DeveloperDesiredVersion])(
          (list, entry) => list :+ DeveloperDesiredVersion(entry._1, entry._2)).sortBy(_.serviceName)
        setDeveloperDesiredVersions(desiredVersionsList)
      }
    } yield result
  }

  def setDeveloperDesiredVersions(desiredVersions: Seq[DeveloperDesiredVersion])(implicit log: Logger): Future[Unit] = {
    log.info(s"Set developer desired versions ${desiredVersions}")
    collections.Developer_DesiredVersions.update(new BsonDocument(), _ => Some(DeveloperDesiredVersions(desiredVersions))).map(_ => ())
  }

  def getDeveloperDesiredVersions(serviceNames: Set[ServiceName])(implicit log: Logger): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      profile <- collections.Developer_DesiredVersions.find(new BsonDocument()).map(_.map(_.versions).headOption.getOrElse(Seq.empty[DeveloperDesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def getDeveloperDesiredVersion(serviceName: ServiceName)(implicit log: Logger): Future[Option[DeveloperDistributionVersion]] = {
    getDeveloperDesiredVersions(Set(serviceName)).map(_.headOption.map(_.version))
  }

  def filterDesiredVersionsByProfile(distributionName: DistributionName, future: Future[Seq[DeveloperDesiredVersion]])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getDistributionClientInstallProfile(distributionName)
      versions <- Future(desiredVersions.filter(version => installProfile.services.contains(version.serviceName)))
    } yield versions
  }

  def getDeveloperDesiredVersions(distributionName: DistributionName, serviceNames: Set[ServiceName])(implicit log: Logger)
      : Future[Seq[DeveloperDesiredVersion]] = {
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

  def downloadUpdates(developerDistributionClient: DistributionClient[AkkaSource], serviceNames: Seq[ServiceName])
                     (implicit log: Logger): Future[Map[ServiceName, DeveloperDistributionVersion]] = {
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

  private def getBusyVersions(distributionName: DistributionName, serviceName: ServiceName)(implicit log: Logger): Future[Set[DeveloperVersion]] = {
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