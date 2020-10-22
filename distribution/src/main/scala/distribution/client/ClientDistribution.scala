package distribution.client

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route.seal
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.client.{ClientDistributionDirectory, ClientDistributionWebPaths}
import com.vyulabs.update.info.ProfiledServiceName
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.client.uploaders.{ClientFaultUploader, ClientLogUploader, ClientStateUploader}
import com.vyulabs.update.info.VersionsInfoJson._
import distribution.Distribution
import distribution.client.config.ClientDistributionConfig
import distribution.graphql.Graphql
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}

import scala.concurrent.ExecutionContext

class ClientDistribution(protected val dir: ClientDistributionDirectory,
                         protected val collections: ClientDatabaseCollections,
                         protected val config: ClientDistributionConfig,
                         protected val usersCredentials: UsersCredentials,
                         protected val graphql: Graphql,
                         protected val stateUploader: ClientStateUploader,
                         protected val logUploader: ClientLogUploader,
                         protected val faultUploader: ClientFaultUploader)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext,
                         protected val filesLocker: SmartFilesLocker)
    extends Distribution(usersCredentials, graphql) with GetUtils with PutUtils with VersionUtils with CommonUtils
      with ClientDistributionWebPaths with SprayJsonSupport {

  implicit val directory = dir

  private val prefix = "update"

  val route: Route =
    path(pingPath) {
      get {
        complete("pong")
      }
    } ~
    logRequest(requestLogger _) {
      logResult(resultLogger _) {
        handleExceptions(exceptionHandler) {
          extractRequestContext { ctx =>
            pathPrefix(prefix / apiPathPrefix) {
              seal {
                mapRejections { rejections => // Prevent browser to invoke basic auth popup.
                  rejections.map(_ match {
                    case AuthenticationFailedRejection(cause, challenge) =>
                      val scheme = if (challenge.scheme == "Basic") "x-Basic" else challenge.scheme
                      AuthenticationFailedRejection(cause, HttpChallenge(scheme, challenge.realm, challenge.params))
                    case rejection =>
                      rejection

                  })
                } {
                  authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, role) =>
                    get {
                      path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                        getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                      } ~
                        path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                        } ~
                        path(versionsInfoPath / ".*".r) { (service) =>
                          complete(getVersionsInfo(service))
                        } ~
                        path(desiredVersionsPath) {
                          getFromFileWithLock(dir.getDesiredVersionsFile())
                        } ~
                        path(desiredVersionPath / ".*".r) { service =>
                          complete(getDesiredVersion(service, getDesiredVersions()))
                        } ~
                        path(servicesStatePath / ".*".r) { (instanceId) =>
                          stateUploader.getInstanceState(instanceId)
                        } ~
                        path(distributionVersionPath) {
                          complete(getVersion())
                        } ~
                        path(scriptsVersionPath) {
                          complete(getServiceVersion(Common.ScriptsServiceName, new File(".")))
                        }
                    } ~
                      post {
                        authorize(role == UserRole.Administrator) {
                          path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                            val buildVersion = BuildVersion.parse(version)
                            versionImageUpload(service, buildVersion)
                          } ~
                            path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              complete(addVersionInfo(service, buildVersion, null))
                            } ~
                            path(desiredVersionsPath) {
                              fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile())
                            }
                        } ~
                          authorize(role == UserRole.Service) {
                            path(servicesStatePath / ".*".r) { instanceId =>
                              uploadFileToJson(servicesStateName, (json) => {
                                // TODO graphql
                                //val servicesState = json.convertTo[ServicesState]
                                //stateUploader.receiveState(instanceId, servicesState)
                                complete(StatusCodes.OK)
                              })
                            } ~
                              path(serviceLogsPath / ".*".r / ".*".r) { (instanceId, profiledServiceName) =>
                                uploadFileToJson(serviceLogsName, (json) => {
                                  val serviceLogs = json.convertTo[ServiceLogs]
                                  onSuccess(logUploader.receiveLogs(instanceId, ProfiledServiceName.parse(profiledServiceName), serviceLogs))(complete(StatusCodes.OK))
                                })
                              } ~
                              path(serviceFaultPath / ".*".r) { (serviceName) =>
                                uploadFileToSource(serviceFaultName, (fileInfo, source) => {
                                  faultUploader.receiveFault(serviceName, fileInfo.getFileName, source)
                                })
                              }
                          }
                      }
                  }
                }
              }
            } ~
              get {
                path(prefix / browsePath) {
                  authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                    authorize(userRole == UserRole.Administrator) {
                      browse(None)
                    }
                  }
                } ~
                  pathPrefix(prefix / browsePath / ".*".r) { path =>
                    authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                      authorize(userRole == UserRole.Administrator) {
                        browse(Some(path))
                      }
                    }
                  }
              } ~
              mapRejections { rejections => // TODO Old API. Remove later.
                // To prevent browser to invoke basic auth popup.
                rejections.map(_ match {
                  case AuthenticationFailedRejection(cause, challenge) =>
                    val scheme = if (challenge.scheme == "Basic") "x-Basic" else challenge.scheme
                    AuthenticationFailedRejection(cause, HttpChallenge(scheme, challenge.realm, challenge.params))
                  case rejection => rejection
                })
              } {
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  log.debug(s"Old API request ${ctx.request.toString()} from ${userName}")
                  get {
                    path(prefix / downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                      getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                    } ~
                      path(prefix / downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                        getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                      } ~
                      path(prefix / downloadVersionsInfoPath / ".*".r) { (service) =>
                        complete(getVersionsInfo(service))
                      } ~
                      path(prefix / downloadDesiredVersionsPath) {
                        getFromFileWithLock(dir.getDesiredVersionsFile())
                      } ~
                      path(prefix / downloadDesiredVersionPath / ".*".r) { service =>
                        complete(getDesiredVersion(service, getDesiredVersions()))
                      } ~
                      path(prefix / downloadInstanceStatePath / ".*".r) { (instanceId) =>
                        stateUploader.getInstanceState(instanceId)
                      } ~
                      authorize(userRole == UserRole.Administrator) {
                        path(prefix / getDistributionVersionPath) {
                          complete(getVersion())
                        } ~
                          path(prefix / getScriptsVersionPath) {
                            complete(getServiceVersion(Common.ScriptsServiceName, new File(".")))
                          }
                      }
                  } ~
                    post {
                      authorize(userRole == UserRole.Administrator) {
                        path(prefix / uploadVersionPath / ".*".r / ".*".r) { (service, version) =>
                          val buildVersion = BuildVersion.parse(version)
                          versionImageUpload(service, buildVersion)
                        } ~
                          path(prefix / uploadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                            val buildVersion = BuildVersion.parse(version)
                            complete(addVersionInfo(service, buildVersion, null))
                          } ~
                          path(prefix / uploadDesiredVersionsPath) {
                            fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile())
                          }
                      } ~
                        authorize(userRole == UserRole.Service) {
                          path(prefix / uploadInstanceStatePath / ".*".r / ".*".r) { (instanceId, updaterProcessId) =>
                            complete(StatusCodes.BadRequest) // New format
                          } ~
                            path(prefix / uploadServiceLogsPath / ".*".r / ".*".r) { (instanceId, profiledServiceName) =>
                              uploadFileToJson(serviceLogsName, (json) => {
                                val serviceLogs = json.convertTo[ServiceLogs]
                                onSuccess(logUploader.receiveLogs(instanceId, ProfiledServiceName.parse(profiledServiceName), serviceLogs))(complete(StatusCodes.OK))
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
    }
}
