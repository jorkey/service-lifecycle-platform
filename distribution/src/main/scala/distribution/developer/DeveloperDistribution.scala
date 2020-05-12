package distribution.developer

import java.io.IOException
import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{Route, ValidationRejection}
import akka.stream.Materializer
import akka.util.ByteString
import com.typesafe.config.{ConfigParseOptions, ConfigSyntax}
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, TestSignature}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.InstancesState
import com.vyulabs.update.users.{UserRole, UsersCredentials}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global

class DeveloperDistribution(dir: DeveloperDistributionDirectory, port: Int, usersCredentials: UsersCredentials,
                            stateUploader: DeveloperStateUploader, faultUploader: DeveloperFaultUploader)
                           (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
      extends Distribution(dir, usersCredentials) with DeveloperDistributionWebPaths {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  def run(): Unit = {
    val route: Route =
      path(pingPath) {
        get {
          complete("pong")
        }
      } ~
      logRequest(requestLogger _) {
        logResult(resultLogger _) {
          extractRequestContext { ctx =>
            implicit val materializer = ctx.materializer

            authenticateBasic(realm = "Distribution", authenticate) { userName =>
              get {
                path(downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                  getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                } ~
                path(downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                  getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                } ~
                path(downloadVersionsInfoPath / ".*".r) { (service) =>
                  complete(Utils.renderConfig(
                    dir.getVersionsInfo(dir.getServiceDir(service, None)).toConfig(), true))
                } ~
                path(downloadVersionsInfoPath / ".*".r / ".*".r) { (service, client) =>
                  complete(Utils.renderConfig(
                    dir.getVersionsInfo(dir.getServiceDir(service, Some(client))).toConfig(), true))
                } ~
                path(downloadDesiredVersionsPath) {
                  getFromFileWithLock(dir.getDesiredVersionsFile(None))
                } ~
                path(downloadDesiredVersionsPath / ".*".r) { client =>
                  getFromFileWithLock(dir.getDesiredVersionsFile(Some(client)))
                } ~
                path(downloadDesiredVersionPath / ".*".r) { service =>
                  getDesiredVersionImage(service)
                } ~
                path(downloadDesiredVersionPath / ".*".r / ".*".r) { (service, client) =>
                  getDesiredVersionImage(service, client)
                } ~
                authorize(usersCredentials.getRole(userName) == UserRole.Administrator) {
                  path(browsePath) {
                    browse(None)
                  } ~
                  pathPrefix(browsePath / ".*".r) { path =>
                    browse(Some(path))
                  } ~
                  path(getDistributionVersionPath) {
                    getVersion()
                  }
                }
              } ~
                post {
                  authorize(usersCredentials.getRole(userName) == UserRole.Administrator) {
                    path(uploadVersionPath / ".*".r / ".*".r) { (service, version) =>
                      val buildVersion = BuildVersion.parse(version)
                      versionImageUpload(service, buildVersion)
                    } ~
                    path(uploadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                      val buildVersion = BuildVersion.parse(version)
                      versionInfoUpload(service, buildVersion)
                    } ~
                    path(uploadDesiredVersionsPath) {
                      fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(None))
                    } ~
                    path(uploadDesiredVersionsPath / ".*".r) { client =>
                      fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(Some(client)))
                    }
                  } ~
                  authorize(usersCredentials.getRole(userName) == UserRole.Client) {
                    path(uploadTestedVersionsPath) {
                      uploadTestedVersions(userName)
                    } ~
                    path(uploadInstancesStatePath / ".*".r) { client =>
                      uploadToConfig(instancesStateName, (config) => {
                        val instancesState = InstancesState(config)
                        stateUploader.receiveInstancesState(client, instancesState)
                      })
                    } ~
                    path(uploadServiceFaultPath / ".*".r) { (serviceName) =>
                      uploadToSource(serviceFaultName, (fileInfo, source) => {
                        faultUploader.receiveFault(userName, serviceName, fileInfo.getFileName, source)
                      })
                    }
                  }
                }
            }
          }
        }
      }
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  private def getDesiredVersionImage(serviceName: ServiceName, clientName: ClientName): Route = {
    val future = getMergedDesiredVersions(serviceName, clientName)
    getDesiredVersionImage(serviceName, future)
  }

  private def getMergedDesiredVersions(serviceName: ServiceName, clientName: ClientName): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    val future = getDesiredVersions(dir.getDesiredVersionsFile())
    future.onComplete { commonDesiredVersions =>
      val future = getDesiredVersions(dir.getDesiredVersionsFile(Some(clientName)))
      future.onComplete { clientDesiredVersions =>
        try {
          val commonConfig = commonDesiredVersions.get.map(_.toConfig())
          val clientConfig = clientDesiredVersions.get.map(_.toConfig())
          val mergedConfig = (commonConfig, clientConfig) match {
            case (Some(commonConfig), Some(clientConfig)) =>
              Some(clientConfig.withFallback(commonConfig))
            case (Some(commonConfig), None) =>
              Some(commonConfig)
            case (None, Some(clientConfig)) =>
              Some(clientConfig)
            case (None, None) =>
              None
          }
          promise.success(mergedConfig.map(DesiredVersions(_)))
        } catch {
          case e: Exception =>
            promise.failure(e)
        }
      }
    }
    promise.future
  }

  private def uploadTestedVersions(clientName: ClientName): Route = {
    uploadToConfig(testedVersionsName, (config) => {
      val future = overwriteFileContentWithLock(dir.getDesiredVersionsFile(None), content => {
        val desiredVersionsConfig = Utils.parseConfigString(content.decodeString("utf8"), ConfigParseOptions.defaults().setSyntax(ConfigSyntax.JSON)).getOrElse {
          throw new IOException(s"Can't parse ${dir.getDesiredVersionsFile(None)}")
        }
        val desiredVersions = DesiredVersions(desiredVersionsConfig)
        val testedVersions = ServicesVersions(config)
        if (desiredVersions.Versions.equals(testedVersions.Versions)) {
          val testRecord = TestSignature(clientName, new Date())
          val testedDesiredVersions = DesiredVersions(desiredVersions.Versions, desiredVersions.TestSignatures :+ testRecord)
          Some(ByteString(Utils.renderConfig(testedDesiredVersions.toConfig(), true).getBytes("utf8")))
        } else {
          None
        }
      })
      onSuccess(future) { result =>
        if (result) {
          complete(StatusCodes.OK)
        } else {
          failWith(new IOException("Current desired versions are not equals tested versions"))
        }
      }
    })
  }
}
