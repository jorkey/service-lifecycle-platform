package distribution.developer

import java.io.{File, IOException}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentType, StatusCodes}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DistributionInfo, InstancesState}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import distribution.developer.config.DeveloperDistributionConfig

import scala.concurrent.ExecutionContext
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.utils.Utils
import distribution.Distribution
import distribution.developer.utils.{ClientsUtils, StateUtils}
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}

class DeveloperDistribution(val dir: DeveloperDistributionDirectory, val config: DeveloperDistributionConfig, usersCredentials: UsersCredentials,
                            stateUploader: DeveloperStateUploader, faultUploader: DeveloperFaultUploader)
                           (implicit val system: ActorSystem, val materializer: Materializer, val filesLocker: SmartFilesLocker)
       extends Distribution(usersCredentials) with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils
          with DeveloperDistributionWebPaths with SprayJsonSupport {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  def run(): Unit = {
    val route: Route =
      get {
        path(pingPath) {
          complete("pong")
        }
      } ~
      logRequest(requestLogger _) {
        logResult(resultLogger _) {
          handleExceptions(exceptionHandler) {
            extractRequestContext { ctx =>
              pathPrefix(apiPathPrefix) {
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
                    get {
                      path(distributionInfoPath) {
                        complete(DistributionInfo(config.name, Utils.getManifestBuildVersion(Common.DistributionServiceName)
                          .getOrElse(BuildVersion.empty), None))
                      }
                    } ~
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      get {
                        path(userInfoPath) {
                          complete(UserInfo(userName, None, userCredentials.role))
                        } ~
                        path(clientsInfoPath) {
                          complete(getClientsInfo())
                        } ~
                        path(instanceVersionsPath) {
                          complete(getOwnInstanceVersions())
                        } ~
                        path(instanceVersionsPath / ".*".r) { clientName =>
                          getClientInstanceVersions(clientName)
                        } ~
                        path(serviceStatePath / ".*".r / ".*".r /  ".*".r / ".*".r) { (clientName, instanceId, directory, service) =>
                          getServiceState(clientName, instanceId, directory, service)
                        } ~
                        path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                        } ~
                        path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                        } ~
                        authorize(userCredentials.role == UserRole.Administrator) {
                          path(versionsInfoPath / ".*".r) { service =>
                            complete(getVersionsInfo(dir.getServiceDir(service, None)))
                          } ~
                          path(versionsInfoPath / ".*".r / ".*".r) { (service, clientName) =>
                            complete(getVersionsInfo(dir.getServiceDir(service, Some(clientName))))
                          } ~
                          path(desiredVersionsPath) { // TODO сделать обработку независимой от роли
                            complete(getDesiredVersions(None))
                          } ~
                          path(desiredVersionsPath / ".*".r) { clientName =>
                            complete(getClientDesiredVersions(clientName))
                          } ~
                          path(installedDesiredVersionsPath / ".*".r) { clientName =>
                            getFromFileWithLock(dir.getInstalledDesiredVersionsFile(clientName))
                          } ~
                          path(desiredVersionPath / ".*".r) { service =>
                            getDesiredVersion(service, getDesiredVersions(None), false)
                          } ~
                          path(distributionVersionPath) {
                            getVersion()
                          } ~
                          path(scriptsVersionPath) {
                            getServiceVersion(Common.ScriptsServiceName, new File("."))
                          }
                        } ~
                        authorize(userCredentials.role == UserRole.Client) {
                          path(clientConfigPath) {
                            getFromFile(dir.getClientConfigFile(userName))
                          } ~
                          path(desiredVersionsPath) {
                            complete(getClientDesiredVersions(userName))
                          } ~
                          path(desiredVersionPath / ".*".r) { service =>
                            getDesiredVersion(service, getClientDesiredVersions(userName), false)
                          } ~
                          path(testedVersionsPath) { // TODO remove
                            complete(getTestedVersionsByClient(userName))
                          }
                        }
                      } ~
                        post {
                          authorize(userCredentials.role == UserRole.Administrator) {
                            path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionImageUpload(service, buildVersion)
                            } ~
                            path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionInfoUpload(service, buildVersion)
                            } ~
                            path(desiredVersionsPath) {
                              fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(None))
                            } ~
                            path(desiredVersionsPath / ".*".r) { clientName =>
                              fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(Some(clientName)))
                            }
                          } ~
                          authorize(userCredentials.role == UserRole.Client) {
                            path(installedDesiredVersionsPath) {
                              fileUploadWithLock(desiredVersionsName, dir.getInstalledDesiredVersionsFile(userName))
                            } ~
                            path(testedVersionsPath) {
                              uploadTestedVersions(userName)
                            } ~
                            path(instancesStatePath) {
                              uploadFileToJson(instancesStateName, (json) => {
                                val instancesState = json.convertTo[InstancesState]
                                stateUploader.receiveInstancesState(userName, instancesState)
                                complete(StatusCodes.OK)
                              })
                            } ~
                            path(serviceFaultPath / ".*".r) { (serviceName) =>
                              uploadFileToSource(serviceFaultName, (fileInfo, source) => {
                                faultUploader.receiveFault(userName, serviceName, fileInfo.getFileName, source)
                              })
                            }
                          }
                        }
                    }
                  }
                }
              } ~ pathPrefixTest("^download.*|^upload.*|^get.*".r) { p => // TODO Old API. Remove later.
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
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      get {
                          path(downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                          } ~
                          path(downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                          } ~
                          authorize(userCredentials.role == UserRole.Administrator) {
                            path(downloadVersionsInfoPath / ".*".r) { service =>
                              parameter("client".?) { clientName =>
                                complete(getVersionsInfo(dir.getServiceDir(service, clientName)))
                              }
                            } ~
                              path(downloadDesiredVersionsPath) {
                                parameter("client".?) { clientName =>
                                  complete(getDesiredVersions(clientName))
                                }
                              } ~
                              path(downloadDesiredVersionPath / ".*".r) { service =>
                                parameter("image".as[Boolean] ? true) { image =>
                                  getDesiredVersion(service, getDesiredVersions(None), image)
                                }
                              } ~
                              path(getDistributionVersionPath) {
                                getVersion()
                              } ~
                              path(getScriptsVersionPath) {
                                getServiceVersion(Common.ScriptsServiceName, new File("."))
                              }
                          } ~
                          authorize(userCredentials.role == UserRole.Client) {
                            path(downloadClientConfigPath) {
                              getFromFile(dir.getClientConfigFile(userName))
                            } ~
                              path(downloadDesiredVersionsPath) {
                                parameter("common".as[Boolean] ? false) { common =>
                                  complete(if (!common) getClientDesiredVersions(userName) else getDesiredVersions(None))
                                }
                              } ~
                              path(downloadDesiredVersionsPath / ".*".r) { client => // TODO deprecated
                                if (client.isEmpty) {
                                  complete(getDesiredVersions(None))
                                } else if (client == userName) {
                                  complete(getClientDesiredVersions(userName))
                                } else {
                                  failWith(new IOException("invalid request"))
                                }
                              } ~
                              path(downloadDesiredVersionPath / ".*".r) { service =>
                                parameter("image".as[Boolean] ? true) { image =>
                                  getDesiredVersion(service, getClientDesiredVersions(userName), image)
                                }
                              }
                          }
                      } ~
                        post {
                          authorize(userCredentials.role == UserRole.Administrator) {
                            path(uploadVersionPath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionImageUpload(service, buildVersion)
                            } ~
                              path(uploadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                                val buildVersion = BuildVersion.parse(version)
                                versionInfoUpload(service, buildVersion)
                              } ~
                              path(uploadDesiredVersionsPath) {
                                parameter("client".?) { clientName =>
                                  fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(clientName))
                                }
                              }
                          } ~
                            authorize(userCredentials.role == UserRole.Client) {
                              path(uploadTestedVersionsPath) {
                                uploadTestedVersions(userName)
                              } ~
                                path(uploadInstancesStatePath) {
                                  complete(StatusCodes.BadRequest) // New format
                                } ~
                                path(uploadServiceFaultPath / ".*".r) { (serviceName) =>
                                  uploadFileToSource(serviceFaultName, (fileInfo, source) => {
                                    faultUploader.receiveFault(userName, serviceName, fileInfo.getFileName, source)
                                  })
                                }
                            }
                        }
                    }
                  }
                }
              } ~
              get {
                path(browsePath) {
                  seal {
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      authorize(userCredentials.role == UserRole.Administrator) {
                        browse(None)
                      }
                    }
                  }
                } ~
                pathPrefix(browsePath / ".*".r) { path =>
                  seal {
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      authorize(userCredentials.role == UserRole.Administrator) {
                        browse(Some(path))
                      }
                    }
                  }
                } ~
                getFromResourceDirectory(uiPathPrefix) ~
                pathPrefix("") {
                  getFromResource(uiPathPrefix + "/index.html", ContentType(`text/html`, `UTF-8`))
                }
              }
            }
          }
        }
      }
    Http().bindAndHandle(route, "0.0.0.0", config.port)
  }
}
