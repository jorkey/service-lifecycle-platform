package distribution.graphql.utils

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info._
import com.vyulabs.update.users.UsersCredentials._
import com.vyulabs.update.utils.JsUtils.MergedJsObject
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{InvalidConfigException, NotFoundException}
import distribution.mongo.{DatabaseCollections, DesiredVersionsDocument, DeveloperVersionInfoDocument, PersonalDesiredVersionsDocument}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait DeveloperVersionUtils extends ClientsUtils with StateUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val versionHistoryConfig: VersionHistoryConfig
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

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
      _ <- removeObsoleteVersions(info.serviceName, info.version.client)
    } yield info).map(_ => true)
  }

  private def removeObsoleteVersions(serviceName: ServiceName, client: Option[ClientName]): Future[Unit] = {
    for {
      versions <- getDeveloperVersionsInfo(serviceName, client)
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

  def removeDeveloperVersion(serviceName: ServiceName, version: BuildVersion): Future[Boolean] = {
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

  def getDeveloperVersionsInfo(serviceName: ServiceName, clientName: Option[ClientName] = None,
                               version: Option[BuildVersion] = None): Future[Seq[DeveloperVersionInfo]] = {
    val serviceArg = Filters.eq("info.serviceName", serviceName)
    val clientArg = clientName.map { clientName => Filters.eq("info.clientName", clientName) }
    val versionArg = version.map { version => Filters.eq("info.version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ clientArg ++ versionArg).asJava)
    for {
      collection <- collections.Developer_VersionsInfo
      info <- collection.find(filters).map(_.map(_.info))
    } yield info
  }

  def setDeveloperDesiredVersions(clientName: Option[ClientName], desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    clientName match {
      case Some(clientName) =>
        setDeveloperPersonalDesiredVersions(clientName, desiredVersions)
      case None =>
        setDeveloperDesiredVersions(desiredVersions)
    }
  }

  def setDeveloperDesiredVersions(desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    log.info(s"Set developer desired versions ${desiredVersions}")
    for {
      collection <- collections.Developer_DesiredVersions
      result <- collection.replace(new BsonDocument(), DesiredVersionsDocument(desiredVersions)).map(_ => true)
    } yield result
  }

  def getDeveloperDesiredVersions(clientName: Option[ClientName], serviceNames: Set[ServiceName], merged: Boolean): Future[Seq[DesiredVersion]] = {
    clientName match {
      case Some(clientName) =>
        getDeveloperPersonalDesiredVersions(clientName, serviceNames, merged)
      case None =>
        getDeveloperDesiredVersions(serviceNames)
    }
  }

  def getDeveloperDesiredVersions(serviceNames: Set[ServiceName]): Future[Seq[DesiredVersion]] = {
    for {
      collection <- collections.Developer_DesiredVersions
      profile <- collection.find(new BsonDocument()).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def getDeveloperPersonalDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName], merged: Boolean): Future[Seq[DesiredVersion]] = {
    if (merged) {
      getMergedDeveloperDesiredVersions(clientName, serviceNames)
    } else {
      getDeveloperPersonalDesiredVersions(clientName)
    }
  }

  def setDeveloperPersonalDesiredVersions(clientName: ClientName, desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    log.info(s"Set developer personal desired versions ${desiredVersions} for client ${clientName}")
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.Developer_PersonalDesiredVersions
      result <- collection.replace(clientArg, PersonalDesiredVersionsDocument(clientName, desiredVersions)).map(_ => true)
    } yield result
  }

  def getDeveloperPersonalDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.Developer_PersonalDesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def getDeveloperDesiredVersion(serviceName: ServiceName): Future[Option[BuildVersion]] = {
    getDeveloperDesiredVersions(Set(serviceName)).map(_.headOption.map(_.buildVersion))
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[Seq[DesiredVersion]]): Future[Seq[DesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getClientInstallProfile(clientName)
      versions <- Future(desiredVersions.filter(version => installProfile.profile.services.contains(version.serviceName)))
    } yield versions
  }

  def getMergedDeveloperDesiredVersions(clientName: ClientName, serviceNames: Set[ServiceName]): Future[Seq[DesiredVersion]] = {
    for {
      clientConfig <- getClientConfig(clientName)
      developerVersions <- { clientConfig.testClientMatch match {
        case Some(testClientMatch) =>
          for {
            testedVersions <- getTestedVersions(clientConfig.installProfile).map(testedVersions => {
              testedVersions match {
                case Some(testedVersions) =>
                  val regexp = testClientMatch.r
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
                    throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by clients ${testClientMatch}")
                  }
                case None =>
                  throw NotFoundException(s"Desired versions for profile ${clientConfig.installProfile} are not tested by anyone")
              }
            })
          } yield testedVersions
        case None =>
          getDeveloperDesiredVersions(serviceNames)
      }}.map(DesiredVersions.toMap(_))
      clientDesiredVersions <- getDeveloperPersonalDesiredVersions(clientName, serviceNames).map(DesiredVersions.toMap(_))
      versions <- Future {
        if (clientConfig.testClientMatch.isDefined && !clientDesiredVersions.isEmpty) {
          throw InvalidConfigException("Client required preliminary testing shouldn't have personal desired versions")
        }
        val developerJson = developerVersions.toJson
        val clientJson = clientDesiredVersions.toJson
        val mergedJson = developerJson.merge(clientJson)
        val mergedVersions = mergedJson.convertTo[Map[ServiceName, BuildVersion]]
        DesiredVersions.fromMap(mergedVersions)
      }
    } yield versions
  }

  private def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    for {
      desiredVersion <- getDeveloperDesiredVersion(serviceName)
      clientsInfo <- getClientsInfo()
      installedVersions <- Future.sequence(clientsInfo.map(client => getInstalledDesiredVersion(client.clientName, serviceName))).map(
        _.flatten.map(_.buildVersion))
      testedVersions <- Future.sequence(clientsInfo.map(client => getTestedVersions(client.clientConfig.installProfile))).map(
        _.flatten.map(_.versions.find(_.serviceName == serviceName).map(_.buildVersion)).flatten)
      busyVersions <- Future(desiredVersion.toSet ++ installedVersions ++ testedVersions)
    } yield busyVersions
  }
}