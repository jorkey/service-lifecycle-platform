package distribution.client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.stream.Materializer
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.client.{ClientDistributionDirectory, ClientDistributionWebPaths}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.users.{UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.client.uploaders.{ClientFaultUploader, ClientLogUploader, ClientStateUploader}
import org.slf4j.LoggerFactory
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.state.UpdaterInstanceState

import com.vyulabs.update.state.UpdaterInstanceState._
import com.vyulabs.update.logs.ServiceLogs._

class ClientDistribution(dir: ClientDistributionDirectory, port: Int, usersCredentials: UsersCredentials,
                         stateUploader: ClientStateUploader, logUploader: ClientLogUploader, faultUploader: ClientFaultUploader)
                        (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
    extends Distribution(dir, usersCredentials) with ClientDistributionWebPaths with SprayJsonSupport {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val prefix = "update"

  def run(): Unit = {
    val route: Route =
      path(pingPath) {
        get {
          complete("pong")
        }
      } ~
      logRequest(requestLogger _) {
        logResult(resultLogger _) {
          handleExceptions(exceptionHandler) {
            get {
              path(prefix / browsePath) {
                authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                  authorize(userCredentials.role == UserRole.Administrator) {
                    browse(None)
                  }
                }
              } ~
              pathPrefix(prefix / browsePath / ".*".r) { path =>
                authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                  authorize(userCredentials.role == UserRole.Administrator) {
                    browse(Some(path))
                  }
                }
              }
            } ~
            mapRejections { rejections =>
              // To prevent browser to invoke basic auth popup.
              rejections.map(_ match {
                case AuthenticationFailedRejection(cause, challenge) =>
                  val scheme = if (challenge.scheme == "Basic") "x-Basic" else challenge.scheme
                  AuthenticationFailedRejection(cause, HttpChallenge(scheme, challenge.realm, challenge.params))
                case rejection => rejection
              })
            } {
              authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                get {
                  path(prefix / downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                    getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                  } ~
                  path(prefix / downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                    getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                  } ~
                  path(prefix / downloadVersionsInfoPath / ".*".r) { (service) =>
                    complete(dir.getVersionsInfo(dir.getServiceDir(service)))
                  } ~
                  path(prefix / downloadDesiredVersionsPath) {
                    getFromFileWithLock(dir.getDesiredVersionsFile())
                  } ~
                  path(prefix / downloadDesiredVersionPath / ".*".r) { service =>
                    parameter("image".as[Boolean] ? true) { image =>
                      getDesiredVersion(service, image)
                    }
                  } ~
                  path(prefix / downloadInstanceStatePath / ".*".r / ".*".r / ".*".r) { (instanceId, updaterDirectory, updaterProcessId) =>
                    getFromFileWithLock(dir.getInstanceStateFile(instanceId, updaterDirectory, updaterProcessId))
                  } ~
                  path(prefix / downloadInstanceStatePath / ".*".r / ".*".r) { (instanceId, updaterProcessId) => // TODO remove
                    getFromFileWithLock(dir.getInstanceStateFile(instanceId, "/", updaterProcessId))
                  } ~
                  authorize(userCredentials.role == UserRole.Administrator) {
                    path(prefix / getDistributionVersionPath) {
                      getVersion()
                    } ~
                    path(prefix / getScriptsVersionPath) {
                      getScriptsVersion()
                    }
                  }
                } ~
                  post {
                    authorize(userCredentials.role == UserRole.Administrator) {
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
                      authorize(userCredentials.role == UserRole.Service) {
                        path(prefix / uploadInstanceStatePath / ".*".r / ".*".r / ".*".r) { (instanceId, updaterDirectory, updaterProcessId) =>
                          uploadFileToJson(instanceStateName, (json) => {
                            val instanceState = json.convertTo[UpdaterInstanceState]
                            stateUploader.receiveState(instanceId, updaterDirectory, updaterProcessId, instanceState, this)
                          })
                        } ~
                        path(prefix / uploadInstanceStatePath / ".*".r / ".*".r) { (instanceId, updaterProcessId) => // TODO remove
                          uploadFileToJson(instanceStateName, (json) => {
                            val instanceState = json.convertTo[UpdaterInstanceState]
                            stateUploader.receiveState(instanceId, "/", updaterProcessId, instanceState, this)
                          })
                        } ~
                        path(prefix / uploadServiceLogsPath / ".*".r / ".*".r) { (instanceId, serviceInstanceName) =>
                          uploadFileToJson(serviceLogsName, (json) => {
                            val serviceLogs = json.convertTo[ServiceLogs]
                            logUploader.receiveLogs(instanceId, ServiceInstanceName.parse(serviceInstanceName), serviceLogs)
                          })
                        } ~
                        path(prefix / uploadServiceFaultPath / ".*".r) { (serviceName) =>
                          uploadFileToSource(serviceFaultName, (fileInfo, source) => {
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
