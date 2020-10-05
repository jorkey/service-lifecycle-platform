package distribution.utils

import java.io.{File, IOException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server.Directives.{complete, failWith, _}
import akka.http.scaladsl.server.{Route, RouteResult}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ServiceName}
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, VersionInfo, VersionsInfo}
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait VersionUtils extends GetUtils with PutUtils with DistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  private val maxVersions = 10

  protected implicit val dir: DistributionDirectory
  protected implicit val executionContext: ExecutionContext

    // TODO remove parameter 'image' when all usages will 'false'
  def getDesiredVersion(serviceName: ServiceName, future: Future[Option[DesiredVersions]], image: Boolean): Route = {
    onSuccess(future) { desiredVersions =>
      desiredVersions match {
        case Some(desiredVersions) =>
          desiredVersions.desiredVersions.get(serviceName) match {
            case Some(version) =>
              if (!image) {
                complete(version.toString)
              } else {
                getFromFileWithLock(dir.getVersionImageFile(serviceName, version))
              }
            case None =>
              complete(NotFound, s"Desired version fot service ${serviceName} is not found")
          }
        case None =>
          complete(NotFound, "Desired versions is not found")
      }
    }
  }

  def versionImageUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    versionUpload(versionName, dir.getVersionImageFile(serviceName, buildVersion))
  }

  def versionInfoUpload(serviceName: ServiceName, buildVersion: BuildVersion): Route = {
    mapRouteResult {
      case result@RouteResult.Complete(_) =>
        log.info(s"Uploaded version ${buildVersion} of service ${serviceName}")
        val versionsDir = dir.getServiceDir(serviceName, buildVersion.client)
        getVersionsInfo(versionsDir).onComplete {
          case Success(versionsInfo) =>
            getBusyVersions(serviceName).onComplete {
              case Success(busyVersions) =>
                var notUsedVersions =
                  versionsInfo.versions.filterNot(
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
    }
  }

  protected def getBusyVersions(serviceName: ServiceName): Future[Set[BuildVersion]] = {
    val promise = Promise.apply[Set[BuildVersion]]()
    parseJsonFileWithLock[DesiredVersions](dir.getDesiredVersionsFile()).onComplete {
      case Success(Some(versions)) =>
        promise.success(versions.desiredVersions.get(serviceName).toSet)
      case Success(None) =>
        promise.success(Set.empty)
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  def getVersionsInfo(directory: File): Future[VersionsInfo] = {
    var versions = Seq.empty[Future[Option[VersionInfo]]]
    if (directory.exists()) {
      for (file <- directory.listFiles()) {
        if (file.getName.endsWith("-info.json")) {
          versions :+= parseJsonFileWithLock[VersionInfo](file)
        }
      }
    }
    val promise = Promise.apply[VersionsInfo]()
    Future.sequence(versions).onComplete {
      case Success(versions) =>
        promise.success(VersionsInfo(versions.flatten))
      case Failure(ex) =>
        promise.failure(ex)
    }
    promise.future
  }

  private def versionUpload(fieldName: String, imageFile: File): Route = {
    val directory = new File(imageFile.getParent)
    if (directory.exists() || directory.mkdir()) {
      fileUploadWithLock(fieldName, imageFile)
    } else {
      failWith(new IOException(s"Can't make directory ${directory}"))
    }
  }

  def getVersion(): Route = {
    Utils.getManifestBuildVersion(Common.DistributionServiceName) match {
      case Some(version) =>
        complete(version.toString)
      case None =>
        complete((InternalServerError, s"Version is not defined in manifest"))
    }
  }

  def getServiceVersion(serviceName: ServiceName, directory: File): Route = {
    IOUtils.readServiceVersion(serviceName, directory) match {
      case Some(version) =>
        complete(version.toString)
      case None =>
        complete((InternalServerError, s"Can't found version of scripts"))
    }
  }
}
