package distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info._
import com.vyulabs.update.version.DeveloperDistributionVersion
import distribution.config.VersionHistoryConfig
import distribution.mongo.{DatabaseCollections, DeveloperDesiredVersionsDocument, DeveloperVersionInfoDocument}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

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
      _ <- removeObsoleteVersions(info.serviceName)
    } yield info).map(_ => true)
  }

  private def removeObsoleteVersions(serviceName: ServiceName): Future[Unit] = {
    for {
      versions <- getDeveloperVersionsInfo(serviceName)
      busyVersions <- getBusyVersions(serviceName)
      complete <- {
        val notUsedVersions = versions.filterNot(version => busyVersions.contains(version.version))
          .sortBy(_.buildInfo.date.getTime).map(_.version)
        if (notUsedVersions.size > versionHistoryConfig.maxSize) {
          Future.sequence(notUsedVersions.take(notUsedVersions.size - versionHistoryConfig.maxSize).map { version =>
            removeDeveloperVersion(serviceName, version)
          })
        } else {
          Future()
        }
      }
    } yield complete
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion): Future[Boolean] = {
    log.info(s"Remove developer version ${version} of service ${serviceName}")
    val filters = Filters.and(
      Filters.eq("info.serviceName", serviceName),
      Filters.eq("info.version", version.toString))
    dir.getDeveloperVersionImageFile(serviceName, version).delete()
    for {
      collection <- collections.Developer_VersionsInfo
      profile <- {
        collection.delete(filters).map(_.getDeletedCount > 0)
      }
    } yield profile
  }

  def getDeveloperVersionsInfo(serviceName: ServiceName, version: Option[DeveloperDistributionVersion] = None): Future[Seq[DeveloperVersionInfo]] = {
    val serviceArg = Filters.eq("info.serviceName", serviceName)
    val versionArg = version.map { version => Filters.eq("info.version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ versionArg).asJava)
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

  /* TODO graphql
  def getMergedDeveloperDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName]): Future[Seq[DeveloperDesiredVersion]] = {
    for {
      clientConfig <- getClientConfig(clientName)
      developerVersions <- { clientConfig.testDistributionMatch match {
        case Some(testDistributionMatch) =>
          for {
            testedVersions <- getTestedVersions(clientConfig.installProfile).map(testedVersions => {
              testedVersions match {
                case Some(testedVersions) =>
                  val regexp = testDistributionMatch.r
                  val testCondition = testedVersions.signatures.exists(signature =>
                    signature.clientName match {
                      case regexp() =>
                        true
                      case _ =>
                        false
                    })
                  if (testCondition) {
                    testedVersions.versions
                  } else {
                    throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by clients ${testDistributionMatch}")
                  }
                case None =>
                  throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by anyone")
              }
            })
          } yield testedVersions
        case None =>
          getDeveloperDesiredVersions(serviceNames)
      }}.map(DeveloperDesiredVersions.toMap(_))
      clientDesiredVersions <- getDeveloperPersonalDesiredVersions(clientName, serviceNames).map(DeveloperDesiredVersions.toMap(_))
      versions <- Future {
        if (clientConfig.testDistributionMatch.isDefined && !clientDesiredVersions.isEmpty) {
          throw InvalidConfigException("Client required preliminary testing shouldn't have personal desired versions")
        }
        val developerJson = developerVersions.toJson
        val clientJson = clientDesiredVersions.toJson
        val mergedJson = developerJson.merge(clientJson)
        val mergedVersions = mergedJson.convertTo[Map[ServiceName, DeveloperDistributionVersion]]
        DeveloperDesiredVersions.fromMap(mergedVersions)
      }
    } yield versions
  }*/

  private def getBusyVersions(serviceName: ServiceName): Future[Set[DeveloperDistributionVersion]] = {
    for {
      desiredVersion <- getDeveloperDesiredVersion(serviceName)
      clientsInfo <- getDistributionClientsInfo()
      installedVersions <- Future.sequence(clientsInfo.map(client => getInstalledDesiredVersion(client.distributionName, serviceName))).map(
        _.flatten.map(_.version.original()))
      testedVersions <- Future.sequence(clientsInfo.map(client => getTestedVersions(client.clientConfig.installProfile))).map(
        _.flatten.map(_.versions.find(_.serviceName == serviceName).map(_.version)).flatten)
      busyVersions <- Future(desiredVersion.toSet ++ installedVersions ++ testedVersions)
    } yield busyVersions
  }
}