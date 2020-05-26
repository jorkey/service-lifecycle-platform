package distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.client.{ClientDistributionDirectory, ClientDistributionWebPaths}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.users.{UserRole, UsersCredentials}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import distribution.client.uploaders.{ClientFaultUploader, ClientLogUploader, ClientStateUploader}
import org.slf4j.LoggerFactory

class ClientDistribution(dir: ClientDistributionDirectory, port: Int, usersCredentials: UsersCredentials,
                         stateUploader: ClientStateUploader, logUploader: ClientLogUploader, faultUploader: ClientFaultUploader)
                        (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
    extends Distribution(dir, usersCredentials) with ClientDistributionWebPaths {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val prefix = "update"

  def run(): Unit = {
    val route: Route =
      path(pingPath) {
        get {
          complete("pong")
        }
      } ~
      handleExceptions(exceptionHandler) {
        logRequest(requestLogger _) {
          logResult(resultLogger _) {
            extractRequestContext { ctx =>
              implicit val materializer = ctx.materializer

              authenticateBasic(realm = "Distribution", authenticate) { userName =>
                get {
                  path(prefix / downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                    getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                  } ~
                    path(prefix / downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                      getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                    } ~
                    path(prefix / downloadVersionsInfoPath / ".*".r) { (service) =>
                      complete(Utils.renderConfig(
                        dir.getVersionsInfo(dir.getServiceDir(service)).toConfig(), true))
                    } ~
                    path(prefix / downloadDesiredVersionsPath) {
                      getFromFileWithLock(dir.getDesiredVersionsFile())
                    } ~
                    path(prefix / downloadDesiredVersionPath / ".*".r) { service =>
                      parameter("image".as[Boolean]?true) { image =>
                        getDesiredVersion(service, image)
                      }
                    } ~
                    path(prefix / downloadInstanceStatePath / ".*".r / ".*".r) { (instanceId, updaterInstanceId) =>
                      getFromFileWithLock(dir.getInstanceStateFile(instanceId, updaterInstanceId))
                    } ~
                    authorize(usersCredentials.getRole(userName) == UserRole.Administrator) {
                      path(prefix / browsePath) {
                        browse(None)
                      } ~
                      pathPrefix(prefix / browsePath / ".*".r) { path =>
                        browse(Some(path))
                      } ~
                      path(prefix / getDistributionVersionPath) {
                        getVersion()
                      } ~
                      path(prefix / getScriptsVersionPath) {
                        getScriptsVersion()
                      }
                    }
                } ~
                  post {
                    authorize(usersCredentials.getRole(userName) == UserRole.Administrator) {
                      path(prefix / uploadVersionPath / ".*".r / ".*".r) { (service, version) =>
                        val buildVersion = BuildVersion.parse(version)
                        versionImageUpload(service, buildVersion)
                      } ~
                        path(prefix / uploadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                          val buildVersion = BuildVersion.parse(version)
                          versionInfoUpload(service, buildVersion)
                        } ~
                        path(prefix / uploadDesiredVersionsPath) {
                          fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile())
                        }
                    } ~
                      authorize(usersCredentials.getRole(userName) == UserRole.Service) {
                        path(prefix / uploadInstanceStatePath / ".*".r / ".*".r) { (instanceId, updaterProcessId) =>
                          uploadToConfig(instanceStateName, (config) => {
                            stateUploader.receiveState(instanceId, updaterProcessId, config, this)
                          })
                        } ~
                          // TODO remove when no need of back compability
                          path(prefix / uploadInstanceStatePath / ".*".r) { (instanceId) =>
                            uploadToConfig(instanceStateName, (config) => {
                              stateUploader.receiveState(instanceId, "x", config, this)
                            })
                          } ~
                          path(prefix / uploadServiceLogsPath / ".*".r / ".*".r) { (instanceId, serviceInstanceName) =>
                            uploadToConfig(serviceLogsName, (config) => {
                              val serviceLogs = ServiceLogs.apply(config)
                              logUploader.receiveLogs(instanceId, ServiceInstanceName.parse(serviceInstanceName), serviceLogs)
                            })
                          } ~
                          path(prefix / uploadServiceFaultPath / ".*".r) { (serviceName) =>
                            uploadToSource(serviceFaultName, (fileInfo, source) => {
                              faultUploader.receiveFault(serviceName, fileInfo.getFileName, source)
                            })
                          }
                      }
                  }
              }
            }
          }
        }
      }
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  protected def getDesiredVersion(serviceName: ServiceName, image: Boolean): Route = {
    val future = getDesiredVersions(dir.getDesiredVersionsFile())
    getDesiredVersion(serviceName, future, image)
  }
}
