package distribution.developer

import java.io.{FileNotFoundException, IOException}
import java.util.Date

import akka.NotUsed
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
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.common.Common.{ClientName, InstallProfileName}
import com.vyulabs.update.config.{ClientConfig, InstallProfile}
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, TestSignature}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.{InstanceState, InstancesState}
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global

import com.vyulabs.update.users.UserInfoJson._
import com.vyulabs.update.config.ClientConfigJson._
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.state.InstancesStateJson._
import com.vyulabs.update.info.DesiredVersionsJson._
import com.vyulabs.update.config.InstallProfileJson._
import com.vyulabs.update.info.ServicesVersionsJson._

import spray.json._

import com.vyulabs.update.utils.JsUtils._

class DeveloperDistribution(dir: DeveloperDistributionDirectory, port: Int, usersCredentials: UsersCredentials,
                            stateUploader: DeveloperStateUploader, faultUploader: DeveloperFaultUploader)
                           (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
      extends Distribution(dir, usersCredentials) with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

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
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      get {
                        path(getUserInfoPath) {
                          complete(UserInfo(userName, None, userCredentials.role))
                        } ~
                        path(getClientsInfoPath) {
                          complete(getClientsInfo())
                        } ~
                        path(getVersionPath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                        } ~
                        path(getVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                        } ~
                        authorize(userCredentials.role == UserRole.Administrator) {
                          path(getVersionsInfoPath / ".*".r) { service =>
                            parameter("client".?) { clientName =>
                              complete(dir.getVersionsInfo(dir.getServiceDir(service, clientName)))
                            }
                          } ~
                          path(getDesiredVersionsPath) {
                            parameter("client".?) { clientName =>
                              getDesiredVersionsRoute(getDesiredVersions(clientName))
                            }
                          } ~
                          path(getDesiredVersionPath / ".*".r) { service =>
                            parameter("image".as[Boolean] ? true) { image =>
                              getDesiredVersion(service, getDesiredVersions(None), image)
                            }
                          } ~
                          path(getDistributionVersionPath) {
                            getVersion()
                          } ~
                          path(getScriptsVersionPath) {
                            getScriptsVersion()
                          }
                        } ~
                        authorize(userCredentials.role == UserRole.Client) {
                          path(getClientConfigPath) {
                            getFromFile(dir.getClientConfigFile(userName))
                          } ~
                          path(getDesiredVersionsPath) {
                            parameter("common".as[Boolean] ? false) { common =>
                              getDesiredVersionsRoute(if (!common) getPersonalDesiredVersions(userName) else getDesiredVersions(None))
                            }
                          } ~
                          path(getDesiredVersionPath / ".*".r) { service =>
                            parameter("image".as[Boolean] ? true) { image =>
                              getDesiredVersion(service, getPersonalDesiredVersions(userName), image)
                            }
                          }
                        }
                      } ~
                        post {
                          authorize(userCredentials.role == UserRole.Administrator) {
                            path(putVersionPath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionImageUpload(service, buildVersion)
                            } ~
                            path(putVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionInfoUpload(service, buildVersion)
                            } ~
                            path(putDesiredVersionsPath) {
                              parameter("client".?) { clientName =>
                                fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(clientName))
                              }
                            }
                          } ~
                          authorize(userCredentials.role == UserRole.Client) {
                            path(putTestedVersionsPath) {
                              uploadTestedVersions(userName)
                            } ~
                            path(putInstancesStatePath) {
                              uploadFileToJson(instancesStateName, (json) => {
                                val instancesState = json.convertTo[InstancesState]
                                stateUploader.receiveInstancesState(userName, instancesState)
                              })
                            } ~
                            path(putServiceFaultPath / ".*".r) { (serviceName) =>
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
                                complete(dir.getVersionsInfo(dir.getServiceDir(service, clientName)))
                              }
                            } ~
                              path(downloadDesiredVersionsPath) {
                                parameter("client".?) { clientName =>
                                  getDesiredVersionsRoute(getDesiredVersions(clientName))
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
                                getScriptsVersion()
                              }
                          } ~
                          authorize(userCredentials.role == UserRole.Client) {
                            path(downloadClientConfigPath) {
                              getFromFile(dir.getClientConfigFile(userName))
                            } ~
                              path(downloadDesiredVersionsPath) {
                                parameter("common".as[Boolean] ? false) { common =>
                                  getDesiredVersionsRoute(if (!common) getPersonalDesiredVersions(userName) else getDesiredVersions(None))
                                }
                              } ~
                              path(downloadDesiredVersionsPath / ".*".r) { client => // TODO deprecated
                                if (client.isEmpty) {
                                  getDesiredVersionsRoute(getDesiredVersions(None))
                                } else if (client == userName) {
                                  getDesiredVersionsRoute(getPersonalDesiredVersions(userName))
                                } else {
                                  failWith(new IOException("invalid request"))
                                }
                              } ~
                              path(downloadDesiredVersionPath / ".*".r) { service =>
                                parameter("image".as[Boolean] ? true) { image =>
                                  getDesiredVersion(service, getPersonalDesiredVersions(userName), image)
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
                                  uploadFileToJson(instancesStateName, (json) => {
                                    val instancesState = json.convertTo[InstancesState]
                                    stateUploader.receiveInstancesState(userName, instancesState)
                                  })
                                } ~
                                path(uploadInstancesStatePath / ".*".r) { client => // TODO deprecated
                                  uploadFileToJson(instancesStateName, (json) => {
                                    val instancesState = json.convertTo[InstancesState]
                                    stateUploader.receiveInstancesState(userName, instancesState)
                                  })
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
    Http().bindAndHandle(route, "0.0.0.0", port)
  }

  private def getDesiredVersionsRoute(future: Future[Option[DesiredVersions]]): Route = {
    onSuccess(future) { desiredVersions =>
      desiredVersions match {
        case Some(desiredVersions) =>
          complete(desiredVersions)
        case None =>
          complete((InternalServerError, s"No desired versions"))
      }
    }
  }

  private def getClientsInfo(): Source[ClientConfig, NotUsed] = {
    Source(dir.getClientsDir().list().toList)
      .map(clientName => getClientConfig(clientName).collect { case Some(config) => (clientName, config) })
      .flatMapConcat(config => Source.future(config))
      .map { case (clientName, config) => ClientConfig(config.installProfile, config.testClientMatch) }
  }

  /* TODO
  private def getInstancesState(): Source[InstanceState, NotUsed] = {
    Source(dir.getClientsDir().list().toList)
      .map(clientName => getClientConfig(clientName).collect { case Some(config) => (clientName, config) })
      .flatMapConcat(config => Source.future(config))
      .map { case (clientName, config) => ClientConfig(config.installProfile, config.testClientMatch) }

  }
   */

  private def getClientConfig(clientName: ClientName): Future[Option[ClientConfig]] = {
    val promise = Promise[Option[ClientConfig]]()
    getFileContentWithLock(dir.getClientConfigFile(clientName)).onComplete { bytes =>
      try {
        val clientConfig = bytes.get.decodeString("utf8").parseJson.convertTo[ClientConfig]
        promise.success(Some(clientConfig))
      } catch {
        case _: FileNotFoundException =>
          promise.success(None)
        case ex: Exception =>
          promise.failure(ex)
      }
    }
    promise.future
  }

  private def getInstallProfile(profileName: InstallProfileName): Future[Option[InstallProfile]] = {
    val promise = Promise[Option[InstallProfile]]()
    val file = dir.getInstallProfileFile(profileName)
    getFileContentWithLock(file).onComplete { bytes =>
      try {
        val installProfile = bytes.get.decodeString("utf8").parseJson.convertTo[InstallProfile]
        promise.success(Some(installProfile))
      } catch {
        case _: FileNotFoundException =>
          promise.success(None)
        case ex: Exception =>
          promise.failure(ex)
      }
    }
    promise.future
  }

  private def getDesiredVersions(clientName: Option[ClientName]): Future[Option[DesiredVersions]] = {
    getDesiredVersions(dir.getDesiredVersionsFile(clientName))
  }

  private def getPersonalDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    val future = getMergedDesiredVersions(clientName)
    future.onComplete { desiredVersions =>
      desiredVersions.get match {
        case Some(desiredVersions) =>
          val future = getClientConfig(clientName)
          future.onComplete { clientConfig =>
            try {
              clientConfig.get match {
                case Some(clientConfig) =>
                  val future = getInstallProfile(clientConfig.installProfile)
                  future.onComplete { installProfile =>
                    try {
                      installProfile.get match {
                        case Some(installProfile) =>
                          val filteredVersions = desiredVersions.desiredVersions.filterKeys(installProfile.services.contains(_))
                          promise.success(Some(DesiredVersions(filteredVersions, desiredVersions.testSignatures)))
                        case None =>
                          promise.failure(new IOException(s"Can't find install profile '${clientConfig.installProfile}''"))
                      }
                    } catch {
                      case e: Exception =>
                        promise.failure(e)
                    }
                  }
                case None =>
                  promise.failure(new IOException(s"Can't find client '${clientName}' config"))
              }
            } catch {
              case e: Exception =>
                promise.failure(e)
            }
          }
        case None =>
          promise.success(None)
      }
    }
    promise.future
  }

  private def getMergedDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
    val future = getDesiredVersions(None)
    future.onComplete { commonDesiredVersions =>
      val future = getDesiredVersions(Some(clientName))
      future.onComplete { clientDesiredVersions =>
        try {
          val commonJson = commonDesiredVersions.get.map(_.toJson)
          val clientJson = clientDesiredVersions.get.map(_.toJson)
          val mergedJson = (commonJson, clientJson) match {
            case (Some(commonJson), Some(clientJson)) =>
              Some(commonJson.merge(clientJson))
            case (Some(commonConfig), None) =>
              Some(commonConfig)
            case (None, Some(clientConfig)) =>
              Some(clientConfig)
            case (None, None) =>
              None
          }
          promise.success(mergedJson.map(_.convertTo[DesiredVersions]))
          null
        } catch {
          case e: Exception =>
            promise.failure(e)
        }
      }
    }
    promise.future
  }

  private def uploadTestedVersions(clientName: ClientName): Route = {
    uploadFileToJson(testedVersionsName, (json) => {
      val future = overwriteFileContentWithLock(dir.getDesiredVersionsFile(None), content => {
        val desiredVersions = content.decodeString("utf8").parseJson.convertTo[DesiredVersions]
        val testedVersions = json.convertTo[ServicesVersions]
        if (desiredVersions.desiredVersions.equals(testedVersions.servicesVersions)) {
          val testRecord = TestSignature(clientName, new Date())
          val testedDesiredVersions = DesiredVersions(desiredVersions.desiredVersions, desiredVersions.testSignatures :+ testRecord)
          Some(ByteString(testedDesiredVersions.toJson.prettyPrint.getBytes("utf8")))
        } else {
          None
        }
      })
      onSuccess(future) { result =>
        if (result) {
          complete(StatusCodes.OK)
        } else {
          failWith(new IOException("Current common desired versions are not equals tested versions"))
        }
      }
    })
  }
}
