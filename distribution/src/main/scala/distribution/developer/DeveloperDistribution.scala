package distribution.developer

import java.io.{File, IOException}

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentType, StatusCodes}
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{DistributionInfo, InstanceServiceState}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.uploaders.{DeveloperFaultUploader, DeveloperStateUploader}
import distribution.developer.config.DeveloperDistributionConfig
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.utils.Utils
import distribution.Distribution
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.developer.utils.{ClientsUtils, StateUtils, VersionUtils}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import distribution.utils.{CommonUtils, GetUtils, PutUtils}
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class DeveloperDistribution(protected val dir: DeveloperDistributionDirectory,
                            protected val collections: DeveloperDatabaseCollections,
                            protected val config: DeveloperDistributionConfig,
                            protected val usersCredentials: UsersCredentials,
                            protected val graphql: Graphql,
                            protected val stateUploader: DeveloperStateUploader,
                            protected val faultUploader: DeveloperFaultUploader)
                           (implicit protected val system: ActorSystem,
                            protected val materializer: Materializer,
                            protected val executionContext: ExecutionContext,
                            protected val filesLocker: SmartFilesLocker)
       extends Distribution(usersCredentials, graphql) with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils
          with DeveloperDistributionWebPaths with SprayJsonSupport {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  val route: Route = {
    get {
      path(pingPath) {
        complete("pong")
      }
    } ~
    logRequest(requestLogger _) {
      logResult(resultLogger _) {
        handleExceptions(exceptionHandler) {
          extractRequestContext { ctx =>
            pathPrefix(graphqlPathPrefix) {
              authenticateBasic(realm = "Distribution", authenticate) { case userInfo =>
                post {
                  entity(as[JsValue]) { requestJson =>
                    val JsObject(fields) = requestJson
                    val JsString(query) = fields("query")
                    QueryParser.parse(query) match {
                      case Success(queryAst) =>
                        val operation = fields.get("operation") collect {
                          case JsString(op) => op
                        }
                        val variables = fields.get("variables") match {
                          case Some(obj: JsObject) => obj
                          case _ => JsObject.empty
                        }
                        val context = new DeveloperGraphqlContext(config, dir, collections, userInfo)
                        complete(graphql.executeQuery(DeveloperGraphqlSchema.SchemaDefinition(userInfo.role),
                          context, queryAst, operation, variables))
                      case Failure(error) =>
                        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
                    }
                  }
                } ~ get {
                  parameters("query", "operation".?, "variables".?) { (query, operation, variables) =>
                    QueryParser.parse(query) match {
                      case Success(queryAst) =>
                        val vars = variables.map(_.parseJson) match {
                          case Some(obj: JsObject) => obj
                          case _ => JsObject.empty
                        }
                        val context = new DeveloperGraphqlContext(config, dir, collections, userInfo)
                        complete(graphql.executeQuery(DeveloperGraphqlSchema.SchemaDefinition(userInfo.role),
                          context, queryAst, operation, vars))
                      case Failure(error) =>
                        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
                    }
                  }
                }
              }
            } ~
            pathPrefix(interactiveGraphqlPathPrefix) {
              getFromResource("graphiql.html")
            } ~
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
                    authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                      get {
                        path(userInfoPath) { // deprecated
                          complete(UserInfo(userName, userRole))
                        } ~
                          path(clientsInfoPath) { // deprecated
                            complete(getClientsInfo())
                          } ~
                          //path(instanceVersionsPath / ".*".r) { clientName => // deprecated
                          //  complete(getClientInstanceVersions(clientName))
                          //} ~
                          //path(serviceStatePath / ".*".r / ".*".r / ".*".r / ".*".r) { (clientName, instanceId, directory, service) => // deprecated
                          //  complete(getServiceState(clientName, instanceId, directory, service))
                          //} ~
                          path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                          } ~
                          path(versionInfoPath / ".*".r / ".*".r) { (service, version) => // deprecated
                            getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                          } ~
                          authorize(userRole == UserRole.Administrator) {
                            path(versionsInfoPath / ".*".r) { service => // deprecated
                              complete(getVersionsInfo(service))
                            } ~
                              path(versionsInfoPath / ".*".r / ".*".r) { (service, clientName) => // deprecated
                                complete(getVersionsInfo(service, clientName = Some(clientName)))
                              } ~
                              path(desiredVersionsPath) { // TODO сделать обработку независимой от роли // deprecated
                                complete(getDesiredVersions())
                              } ~
                              path(desiredVersionsPath / ".*".r) { clientName => // deprecated
                                complete(getClientDesiredVersions(clientName))
                              } ~
                              path(installedDesiredVersionsPath / ".*".r) { clientName => // deprecated
                                complete(getInstalledVersions(clientName))
                              } ~
                              //path(desiredVersionPath / ".*".r) { service => // deprecated
                              //  complete(getDesiredVersion(service, getDesiredVersions()))
                              //} ~
                              path(distributionVersionPath) { // deprecated
                                complete(getVersion())
                              } ~
                              path(scriptsVersionPath) { // deprecated
                                complete(getServiceVersion(Common.ScriptsServiceName, new File(".")))
                              }
                          } ~
                          authorize(userRole == UserRole.Client) {
                            path(clientConfigPath) {
                              complete(getClientConfig(userName)) // deprecated
                            } ~
                              path(desiredVersionsPath) {
                                complete(getClientDesiredVersions(userName)) // deprecated
                              } //~
                              //path(desiredVersionPath / ".*".r) { service => // deprecated
                              //  complete(getDesiredVersion(service, getClientDesiredVersions(userName)))
                              //}
                          }
                      } ~
                        post {
                          authorize(userRole == UserRole.Administrator) {
                            path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionImageUpload(service, buildVersion)
                            } ~
                              path(versionInfoPath / ".*".r / ".*".r) { (service, version) =>
                                val buildVersion = BuildVersion.parse(version)
                                complete(addVersionInfo(service, buildVersion, null))
                              } //~
                              //path(desiredVersionsPath) {
                              //  fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(None))
                              //} ~
                              //path(desiredVersionsPath / ".*".r) { clientName =>
                              //  fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(Some(clientName)))
                              //}
                          } ~
                            authorize(userRole == UserRole.Client) {
                              path(installedDesiredVersionsPath) {
                                fileUploadWithLock(desiredVersionsName, dir.getInstalledDesiredVersionsFile(userName))
                              } ~
                                path(testedVersionsPath) {
                                  uploadTestedVersions(userName)
                                } ~
                                path(instancesStatePath) {
                                  uploadFileToJson(instancesStateName, (json) => {
                                    /* TODO graphql
                                    val instancesState = json.convertTo[InstanceServiceState]
                                    stateUploader.receiveServicesState(userName, instancesState)
                                     */
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
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  get {
                      path(downloadVersionPath / ".*".r / ".*".r) { (service, version) =>
                        getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                      } ~
                      path(downloadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                        getFromFile(dir.getVersionInfoFile(service, BuildVersion.parse(version)))
                      } ~
                      authorize(userRole == UserRole.Administrator) {
                        path(downloadVersionsInfoPath / ".*".r) { service =>
                          parameter("client".?) { clientName =>
                            complete(getVersionsInfo(service, clientName = clientName))
                          }
                        } ~
                          path(downloadDesiredVersionsPath) {
                              parameter("client".?) { _ match {
                                case Some(clientName) =>
                                  complete(getClientDesiredVersions(clientName))
                                case None =>
                                  complete(getDesiredVersions())
                              }
                            }
                          } ~
                          //path(downloadDesiredVersionPath / ".*".r) { service =>
                          //  complete(getDesiredVersion(service, getDesiredVersions()))
                          //} ~
                          path(getDistributionVersionPath) {
                            complete(getVersion())
                          } ~
                          path(getScriptsVersionPath) {
                            complete(getServiceVersion(Common.ScriptsServiceName, new File(".")))
                          }
                      } ~
                      authorize(userRole == UserRole.Client) {
                        path(downloadClientConfigPath) {
                          getFromFile(dir.getClientConfigFile(userName))
                        } ~
                          //path(downloadDesiredVersionsPath) {
                          //  parameter("common".as[Boolean] ? false) { common =>
                          //    complete(if (!common) getClientDesiredVersions(userName, true) else getDesiredVersions())
                          //  }
                          //} ~
                          path(downloadDesiredVersionsPath / ".*".r) { client => // TODO deprecated
                            if (client.isEmpty) {
                              complete(getDesiredVersions())
                            } else if (client == userName) {
                              complete(getClientDesiredVersions(userName))
                            } else {
                              failWith(new IOException("invalid request"))
                            }
                          } //~
                          //path(downloadDesiredVersionPath / ".*".r) { service =>
                          //  complete(getDesiredVersion(service, getClientDesiredVersions(userName)))
                          //}
                      }
                  } ~
                    post {
                      authorize(userRole == UserRole.Administrator) {
                        path(uploadVersionPath / ".*".r / ".*".r) { (service, version) =>
                          val buildVersion = BuildVersion.parse(version)
                          versionImageUpload(service, buildVersion)
                        } ~
                          path(uploadVersionInfoPath / ".*".r / ".*".r) { (service, version) =>
                            val buildVersion = BuildVersion.parse(version)
                            complete(addVersionInfo(service, buildVersion, null))
                          } //~
                          //path(uploadDesiredVersionsPath) {
                          //  parameter("client".?) { clientName =>
                          //    fileUploadWithLock(desiredVersionsName, dir.getDesiredVersionsFile(clientName))
                          //  }
                          //}
                      } ~
                        authorize(userRole == UserRole.Client) {
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
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  authorize(userRole == UserRole.Administrator) {
                    browse(None)
                  }
                }
              }
            } ~
            pathPrefix(browsePath / ".*".r) { path =>
              seal {
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  authorize(userRole == UserRole.Administrator) {
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
}
