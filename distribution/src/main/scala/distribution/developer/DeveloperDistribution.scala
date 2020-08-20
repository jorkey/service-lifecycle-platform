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
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, InstallProfileName, ServiceName, VmInstanceId}
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, DistributionInfo, ServicesVersions, TestSignature}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.{VmInstanceVersionsState, VmInstancesState}
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import distribution.developer.config.DeveloperDistributionConfig
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global
import spray.json._
import com.vyulabs.update.utils.JsUtils._
import com.vyulabs.update.config.ClientConfig._
import com.vyulabs.update.config.ClientInfo._
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.state.VmInstancesState._
import com.vyulabs.update.state.VmInstanceVersionsState._
import com.vyulabs.update.info.DesiredVersions._
import com.vyulabs.update.config.InstallProfile._
import com.vyulabs.update.info.ServicesVersions._
import com.vyulabs.update.utils.Utils

class DeveloperDistribution(dir: DeveloperDistributionDirectory, config: DeveloperDistributionConfig, usersCredentials: UsersCredentials,
                            stateUploader: DeveloperStateUploader, faultUploader: DeveloperFaultUploader)
                           (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
      extends Distribution(dir, usersCredentials) with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  def run(): Unit = {
    val route: Route =
      get {
        path(pingPath) {
          complete("pong")
        } ~
        path(distributionInfoPath) {
          complete(DistributionInfo(config.name, Utils.getManifestBuildVersion(Common.DistributionServiceName)
            .getOrElse(BuildVersion.empty)))
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
                    authenticateBasic(realm = "Distribution", authenticate) { case (userName, userCredentials) =>
                      get {
                        path(userInfoPath) {
                          complete(UserInfo(userName, None, userCredentials.role))
                        } ~
                        path(clientsInfoPath) {
                          complete(getClientsInfo())
                        } ~
                        path(instanceVersionsPath / ".*".r) { clientName =>
                          getInstanceVersionsState(clientName)
                        } ~
                        path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                        } ~
                        path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                          getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                        } ~
                        authorize(userCredentials.role == UserRole.Administrator) {
                          path(versionsInfoPath / ".*".r) { service =>
                            complete(dir.getVersionsInfo(dir.getServiceDir(service, None)))
                          } ~
                          path(versionsInfoPath / ".*".r / ".*".r) { (service, clientName) =>
                            complete(dir.getVersionsInfo(dir.getServiceDir(service, Some(clientName))))
                          } ~
                          path(desiredVersionsPath) {
                            getDesiredVersionsRoute(getDesiredVersions(None))
                          } ~
                          path(desiredVersionsPath / ".*".r) { clientName =>
                            getDesiredVersionsRoute(getClientDesiredVersions(clientName))
                          } ~
                          path(desiredVersionPath / ".*".r) { service =>
                            getDesiredVersion(service, getDesiredVersions(None), false)
                          } ~
                          path(distributionVersionPath) {
                            getVersion()
                          } ~
                          path(scriptsVersionPath) {
                            getScriptsVersion()
                          }
                        } ~
                        authorize(userCredentials.role == UserRole.Client) {
                          path(clientConfigPath) {
                            getFromFile(dir.getClientConfigFile(userName))
                          } ~
                          path(desiredVersionsPath) {
                            parameter("common".as[Boolean] ? false) { common =>
                              getDesiredVersionsRoute(if (!common) getClientDesiredVersions(userName) else getDesiredVersions(None))
                            }
                          } ~
                          path(desiredVersionPath / ".*".r) { service =>
                            getDesiredVersion(service, getClientDesiredVersions(userName), false)
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
                            path(testedVersionsPath) {
                              uploadTestedVersions(userName)
                            } ~
                            path(instancesStatePath) {
                              uploadFileToJson(instancesStateName, (json) => {
                                val instancesState = json.convertTo[VmInstancesState]
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
                                  getDesiredVersionsRoute(if (!common) getClientDesiredVersions(userName) else getDesiredVersions(None))
                                }
                              } ~
                              path(downloadDesiredVersionsPath / ".*".r) { client => // TODO deprecated
                                if (client.isEmpty) {
                                  getDesiredVersionsRoute(getDesiredVersions(None))
                                } else if (client == userName) {
                                  getDesiredVersionsRoute(getClientDesiredVersions(userName))
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

  private def getClientsInfo(): Source[ClientInfo, NotUsed] = {
    Source.future(
      Source(dir.getClientsDir().list().toList)
        .map(clientName => getClientConfig(clientName).map {
          case Some(config) => Some(ClientInfo(clientName, config.installProfile, config.testClientMatch))
          case None => None
        })
        .flatMapConcat(config => Source.future(config))
        .runFold(Seq.empty[ClientInfo])((seq, info) => seq ++ info))
        .flatMapConcat(clients => Source.fromIterator(() => clients.iterator))
  }

  private def getInstanceVersionsState(clientName: ClientName): Route = {
     onSuccess(getClientInstancesState(clientName).collect {
        case Some(state) =>
          var versions = Map.empty[ServiceName, Map[BuildVersion, Set[VmInstanceId]]]
          state.state.foreach { case (instanceId, instanceState) =>
            instanceState.values.foreach { updaterState =>
              updaterState.servicesStates.foreach { serviceState =>
                for (version <- serviceState.version) {
                  val serviceName = serviceState.serviceInstanceName.serviceName
                  var map = versions.getOrElse(serviceName, Map.empty[BuildVersion, Set[VmInstanceId]])
                  map += (version -> (map.getOrElse(version, Set.empty) + instanceId))
                  versions += (serviceName -> map)
                }
              }
            }
          }
          VmInstanceVersionsState(versions)
       }) { state => complete(state) }
  }

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

  private def getClientInstancesState(clientName: ClientName): Future[Option[VmInstancesState]] = {
    val promise = Promise[Option[VmInstancesState]]()
    getFileContentWithLock(dir.getInstancesStateFile(clientName)).onComplete { bytes =>
      try {
        val instancesState = bytes.get.decodeString("utf8").parseJson.convertTo[VmInstancesState]
        promise.success(Some(instancesState))
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

  private def getClientDesiredVersions(clientName: ClientName): Future[Option[DesiredVersions]] = {
    filterDesiredVersionsByProfile(clientName, getMergedDesiredVersions(clientName))
  }

  private def filterDesiredVersionsByProfile(clientName: ClientName,
                                             future: Future[Option[DesiredVersions]]): Future[Option[DesiredVersions]] = {
    val promise = Promise[Option[DesiredVersions]]()
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
          val testSignatures = desiredVersions.testSignatures.getOrElse(Seq.empty) :+ testRecord
          val testedDesiredVersions = DesiredVersions(desiredVersions.desiredVersions, Some(testSignatures))
          Some(ByteString(testedDesiredVersions.toJson.sortedPrint.getBytes("utf8")))
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
