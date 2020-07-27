package distribution.developer

import java.io.{FileNotFoundException, IOException}
import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentType, StatusCodes}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.stream.Materializer
import akka.util.ByteString
import com.typesafe.config.{ConfigParseOptions, ConfigSyntax}
import com.vyulabs.update.common.Common.{ClientName, InstallProfileName}
import com.vyulabs.update.config.{ClientConfig, InstallProfile}
import com.vyulabs.update.distribution.Distribution
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, TestSignature}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.state.InstancesState
import com.vyulabs.update.users.{UserRole, UsersCredentials}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import distribution.{JsonSupport, UserInfo}
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, Promise}
import ExecutionContext.Implicits.global

class DeveloperDistribution(dir: DeveloperDistributionDirectory, port: Int, usersCredentials: UsersCredentials,
                            stateUploader: DeveloperStateUploader, faultUploader: DeveloperFaultUploader)
                           (implicit filesLocker: SmartFilesLocker, system: ActorSystem, materializer: Materializer)
      extends Distribution(dir, usersCredentials) with DeveloperDistributionWebPaths with JsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

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
              pathPrefixTest("^login|^download.*|^upload.*|^get.*".r) { p => // TODO remove with new API
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
                        path(loginPath) {
                          complete(UserInfo(userCredentials.role.toString))
                        } ~
                          path(downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                          } ~
                          path(downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                          } ~
                          authorize(userCredentials.role == UserRole.Administrator) {
                            path(downloadVersionsInfoPath / ".*".r) { service =>
                              parameter("client".?) { clientName =>
                                complete(Utils.renderConfig(
                                  dir.getVersionsInfo(dir.getServiceDir(service, clientName)).toConfig(), true))
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
                                  uploadFileToConfig(instancesStateName, (config) => {
                                    val instancesState = InstancesState(config)
                                    stateUploader.receiveInstancesState(userName, instancesState)
                                  })
                                } ~
                                path(uploadInstancesStatePath / ".*".r) { client => // TODO deprecated
                                  uploadFileToConfig(instancesStateName, (config) => {
                                    val instancesState = InstancesState(config)
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
                getFromResourceDirectory("ui") ~
                  pathPrefix("") {
                    getFromResource("index.html", ContentType(`text/html`, `UTF-8`))
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
          complete(Utils.renderConfig(desiredVersions.toConfig(), true))
        case None =>
          complete((InternalServerError, s"No desired versions"))
      }
    }
  }

  private def getClientConfig(clientName: ClientName): Future[Option[ClientConfig]] = {
    val promise = Promise[Option[ClientConfig]]()
    val file = dir.getClientConfigFile(clientName)
    getFileContentWithLock(dir.getClientConfigFile(clientName)).onComplete { bytes =>
      try {
        Utils.parseConfigString(bytes.get.decodeString("utf8")) match {
          case Some(config) =>
            val clientConfig = ClientConfig.apply(config)
            promise.success(Some(clientConfig))
          case None =>
            promise.failure(new IOException(s"Can't parse config file ${file}"))
        }
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
        Utils.parseConfigString(bytes.get.decodeString("utf8")) match {
          case Some(config) =>
            val installProfile = InstallProfile.apply(config)
            promise.success(Some(installProfile))
          case None =>
            promise.failure(new IOException(s"Can't parse config file ${file}"))
        }
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
                  val future = getInstallProfile(clientConfig.installProfileName)
                  future.onComplete { installProfile =>
                    try {
                      installProfile.get match {
                        case Some(installProfile) =>
                          val filteredVersions = desiredVersions.Versions.filterKeys(installProfile.serviceNames.contains(_))
                          promise.success(Some(DesiredVersions(filteredVersions, desiredVersions.TestSignatures)))
                        case None =>
                          promise.failure(new IOException(s"Can't find install profile '${clientConfig.installProfileName}''"))
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
    uploadFileToConfig(testedVersionsName, (config) => {
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
          failWith(new IOException("Current common desired versions are not equals tested versions"))
        }
      }
    })
  }
}
