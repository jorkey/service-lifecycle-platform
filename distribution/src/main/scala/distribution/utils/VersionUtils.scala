package distribution.utils

import java.io.{File, IOException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server.Directives.{complete, failWith, _}
import akka.http.scaladsl.server.{Route, RouteResult}
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionWebPaths}
import com.vyulabs.update.info.{BuildVersionInfo, ClientFaultReport, DesiredVersions, VersionInfo, VersionsInfo}
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import distribution.graphql.NotFoundException
import distribution.mongo.MongoDb
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait VersionUtils extends GetUtils with PutUtils with DistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  private val maxVersions = 10

  protected val dir: DistributionDirectory
  protected val mongoDb: MongoDb

  protected implicit val executionContext: ExecutionContext

  def getDesiredVersions(): Future[DesiredVersions] = {
    for {
      collection <- mongoDb.getOrCreateCollection[DesiredVersions]()
      profile <- collection.find(new BsonDocument()).map(_.headOption.getOrElse(throw NotFoundException("Desired versions are not found")))
    } yield profile
  }

  def getDesiredVersion(serviceName: ServiceName, future: Future[DesiredVersions]): Future[BuildVersion] = {
    future.map(_.versions.get(serviceName).getOrElse(throw NotFoundException(s"Service ${serviceName} is not defined in the desired versions")))
  }

  def versionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    versionUpload(versionName, dir.getVersionImageFile(serviceName, buildVersion))
  }

  def versionInfoUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    /* TODO graphql
    mapRouteResult {
      case result@RouteResult.Complete(_) =>
        log.info(s"Uploaded version ${buildVersion} of service ${serviceName}")
        val versionsDir = dir.getServiceDir(serviceName, buildVersion.client)
        getVersionsInfo(versionsDir).onComplete {
          case Success(versions) =>
            getBusyVersions(serviceName).onComplete {
              case Success(busyVersions) =>
                var notUsedVersions =
                  versions.filterNot(
                    version => { version.version == buildVersion ||
                      busyVersions.contains(version.version) }).sortBy(_.date.getTime).map(_.version)
                log.info("Not used versions " + notUsedVersions)
                while (notUsedVersions.size > maxVersions) {
                  val lastVersion = notUsedVersions.head
                  log.info(s"Remove obsolete version ${lastVersion}")
                  dir.removeVersion(serviceName, lastVersion)
                  notUsedVersions = notUsedVersions.drop(1)
                }
              case Failure(ex) =>
                log.error("Getting busy versions error", ex)
            }
          case Failure(ex) =>
            log.error("Getting versions info error", ex)
        }
        result
      case result =>
        log.info(s"Result ${result}")
        result
    } {
      versionUpload(versionInfoName, dir.getVersionInfoFile(serviceName, buildVersion))
    }*/
    complete(StatusCodes.OK)
  }

  protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    getDesiredVersion(serviceName, getDesiredVersions()).map(Set(_)).recover { case _ => Set.empty[BuildVersion] }
  }

  def getVersionsInfo(serviceName: ServiceName, clientName: Option[ClientName] = None,
                      version: Option[BuildVersion] = None): Future[Seq[VersionInfo]] = {
    val serviceArg = Filters.eq("serviceName", serviceName)
    val clientArg = Filters.eq("clientName", clientName.getOrElse(null))
    val versionArg = version.map { version => Filters.eq("version", version.toString) }
    val filters = Filters.and((Seq(serviceArg, clientArg) ++ versionArg).asJava)
    for {
      collection <- mongoDb.getOrCreateCollection[VersionInfo]()
      info <- collection.find(filters)
    } yield info
  }

  private def versionUpload(fieldName: String, imageFile: File): Route = {
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock(fieldName, imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  def getVersion(): Option[BuildVersion] = {
    Utils.getManifestBuildVersion(Common.DistributionServiceName)
  }

  def getServiceVersion(serviceName: ServiceName, directory: File): BuildVersion = {
    IoUtils.readServiceVersion(serviceName, directory).getOrElse(throw NotFoundException(s"Can't found service ${serviceName} version file")) // TODO move to async
  }
}
