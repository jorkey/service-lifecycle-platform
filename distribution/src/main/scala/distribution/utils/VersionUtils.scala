package distribution.utils

import java.io.{File, IOException}

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import akka.http.scaladsl.server.Directives.{complete, failWith, _}
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ServiceName, UserName}
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionWebPaths}
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

import scala.concurrent.{Future}

trait VersionUtils extends GetUtils with PutUtils with DistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  private val maxVersions = 10

  implicit val dir: DistributionDirectory

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
        log.info("Existing versions " + dir.getVersionsInfo(versionsDir).versions.map(_.version))
        var versions = dir.getVersionsInfo(versionsDir).versions.sortBy(_.date.getTime).map(_.version)
        log.info(s"Versions count is ${versions.size}")
        while (versions.size > maxVersions) {
          val lastVersion = versions.head
          log.info(s"Remove obsolete version ${lastVersion}")
          dir.removeVersion(serviceName, lastVersion)
          versions = dir.getVersionsInfo(versionsDir).versions.sortBy(_.date.getTime).map(_.version)
        }
        result
      case result =>
        log.info(s"Result ${result}")
        result
    } {
      versionUpload(versionInfoName, dir.getVersionInfoFile(serviceName, buildVersion))
    }
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
