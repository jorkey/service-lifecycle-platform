package distribution.utils

import java.io.{File, IOException}
import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives.failWith
import akka.http.scaladsl.server.Route
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info._
import com.vyulabs.update.users.UsersCredentials._
import com.vyulabs.update.utils.JsUtils.MergedJsObject
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import distribution.DatabaseCollections
import distribution.config.VersionHistoryConfig
import distribution.graphql.{InvalidConfigException, NotFoundException}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future, Promise}

trait DeveloperVersionUtils extends ClientsUtils with GetUtils with PutUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val versionHistoryConfig: VersionHistoryConfig
  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  protected implicit val executionContext: ExecutionContext

  def developerVersionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    val imageFile = dir.getDeveloperVersionImageFile(serviceName, buildVersion)
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock("version", imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  def addDeveloperVersionInfo(serviceName: ServiceName, version: BuildVersion, buildInfo: BuildInfo): Future[DeveloperVersionInfo] = {
    log.info(s"Add developer service ${serviceName} version ${version} info ${buildInfo} ")
    for {
      collection <- collections.Developer_VersionsInfo
      versionInfo <- {
        val versionInfo = DeveloperVersionInfo(serviceName, version.client, version, buildInfo)
        collection.insert(versionInfo).map(_ => versionInfo)
      }
      _ <- removeObsoleteVersions(serviceName, version.client)
    } yield versionInfo
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
      Filters.eq("serviceName", serviceName),
      Filters.eq("version", version.toString))
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
    val serviceArg = Filters.eq("serviceName", serviceName)
    val clientArg = clientName.map { clientName => Filters.eq("clientName", clientName) }
    val versionArg = version.map { version => Filters.eq("version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ clientArg ++ versionArg).asJava)
    for {
      collection <- collections.Developer_VersionsInfo
      info <- collection.find(filters)
    } yield info
  }

  def setDeveloperDesiredVersions(clientName: Option[ClientName], desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    log.info(s"Set desired versions ${desiredVersions}")
    for {
      collection <- collections.Developer_DesiredVersions
      result <- collection.replace(new BsonDocument(), ClientDesiredVersions(clientName, desiredVersions)).map(_ => true)
    } yield result
  }

  def getDeveloperDesiredVersions(clientName: Option[ClientName] = None, serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    val clientArg = clientName.map(Filters.eq("clientName", _)).getOrElse(new BsonDocument())
    for {
      collection <- collections.Developer_DesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion])
        .filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)))
    } yield profile
  }

  def addClientVersionInfo(serviceName: ServiceName, version: BuildVersion, buildInfo: BuildInfo, installInfo: InstallInfo): Future[InstalledVersionInfo] = {
    log.info(s"Add client service ${serviceName} version ${version} build info ${buildInfo} install info ${installInfo}")
    for {
      collection <- collections.ClientsVersionsInfo
      versionInfo <- {
        val versionInfo = InstalledVersionInfo(None, serviceName, version, buildInfo, installInfo)
        collection.insert(versionInfo).map(_ => versionInfo)
      }
      _ <- removeObsoleteVersions(serviceName, version.client)
    } yield versionInfo
  }

  def getClientVersionsInfo(serviceName: ServiceName, clientName: Option[ClientName] = None, version: Option[BuildVersion] = None): Future[Seq[InstalledVersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val clientArg = clientName.map { client => Filters.eq("client", client) }
    val versionArg = version.map { version => Filters.eq("version", version.toString) }
    val filters = Filters.and((Seq(serviceArg) ++ clientArg ++ versionArg).asJava)
    for {
      collection <- collections.ClientsVersionsInfo
      info <- collection.find(filters)
    } yield info
  }

  def getClientDesiredVersions(clientName: Option[ClientName] = None, serviceNames: Set[ServiceName] = Set.empty): Future[Seq[DesiredVersion]] = {
    val clientArg = clientName.map { client => Filters.eq("client", client) }
    val filters = clientArg.getOrElse(new BsonDocument())
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(filters).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
        .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
    } yield profile
  }

  def getDeveloperDesiredVersion(serviceName: ServiceName): Future[Option[BuildVersion]] = {
    getDeveloperDesiredVersions(None, Set(serviceName)).map(_.headOption.map(_.buildVersion))
  }

  def getVersion(): Option[BuildVersion] = {
    Utils.getManifestBuildVersion(Common.DistributionServiceName)
  }

  def getDeveloperDesiredVersions(clientName: Option[ClientName], serviceNames: Set[ServiceName], merged: Boolean): Future[Seq[DesiredVersion]] = {
    clientName match {
      case Some(clientName) if merged  =>
        getMergedDeveloperDesiredVersions(clientName, serviceNames)
      case _ =>
        getDeveloperDesiredVersions(clientName, serviceNames)
    }
  }

  def setClientDesiredVersions(clientName: Option[ClientName], desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    for {
      collection <- collections.Client_DesiredVersions
      result <- collection.replace(new BsonDocument(), ClientDesiredVersions(clientName, desiredVersions)).map(_ => true)
    } yield result
  }

  def getClientDesiredVersions(clientName: ClientName): Future[Seq[DesiredVersion]] = {
    val clientArg = Filters.eq("clientName", clientName)
    for {
      collection <- collections.Client_DesiredVersions
      profile <- collection.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[DesiredVersion]))
    } yield profile
  }

  def setTestedVersions(clientName: ClientName, desiredVersions: Seq[DesiredVersion]): Future[Boolean] = {
    for {
      clientConfig <- getClientConfig(clientName)
      testedVersions <- getTestedVersions(clientConfig.installProfile)
      result <- {
        val testRecord = TestSignature(clientName, new Date())
        val testSignatures = testedVersions match {
          case Some(testedVersions) if testedVersions.versions.equals(desiredVersions) =>
            testedVersions.signatures :+ testRecord
          case _ =>
            Seq(testRecord)
        }
        val newTestedVersions = TestedVersions(clientConfig.installProfile, desiredVersions, testSignatures)
        for {
          collection <- collections.State_TestedVersions
          result <- collection.replace(new BsonDocument(), newTestedVersions).map(_ => true)
        } yield result
      }
    } yield result
  }

  def getTestedVersions(profileName: ProfileName): Future[Option[TestedVersions]] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.State_TestedVersions
      profile <- collection.find(profileArg).map(_.headOption)
    } yield profile
  }

  def filterDesiredVersionsByProfile(clientName: ClientName, future: Future[Seq[DesiredVersion]]): Future[Seq[DesiredVersion]] = {
    for {
      desiredVersions <- future
      installProfile <- getClientInstallProfile(clientName)
      versions <- Future(desiredVersions.filter(version => installProfile.services.contains(version.serviceName)))
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
          getDeveloperDesiredVersions()
      }}.map(DesiredVersions.toMap(_))
      clientDesiredVersions <- getDeveloperDesiredVersions(Some(clientName), serviceNames).map(DesiredVersions.toMap(_))
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

  def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val desiredVersion = getDeveloperDesiredVersion(serviceName)
    /* TODO graphql
    val clientDesiredVersions = dir.getClients().map { clientName =>
      getClientDesiredVersion(clientName, serviceName, true)
    }
    val testedVersions = dir.getProfiles().map { profileName =>
      getTestedVersions(profileName).map(_.map(_.versions.find(_.serviceName == serviceName)).flatten.map(_.buildVersion))
    }*/
    val promise = Promise.apply[Set[BuildVersion]]()
    /* TODO graphql
    Future.sequence(Set(desiredVersion) ++ clientDesiredVersions ++ testedVersions).onComplete {
      case Success(versions) =>
        promise.success(versions.flatten)
      case Failure(ex) =>
        promise.failure(ex)
    }
    */
    promise.future
  }
}
