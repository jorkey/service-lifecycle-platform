package distribution

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.{ContentType, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{AuthenticationFailedRejection, ExceptionHandler, Route, RouteResult}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.vyulabs.update.common.Common.DistributionName
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.users.{PasswordHash, UserInfo, UserRole, UsersCredentials}
import distribution.config.{VersionHistoryConfig}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.loaders.{FaultDownloader}
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import distribution.mongo.DatabaseCollections
import org.slf4j.LoggerFactory

class Distribution(distributionName: DistributionName, versionHistoryConfig: VersionHistoryConfig,
                   collections: DatabaseCollections, dir: DistributionDirectory,
                   usersCredentials: UsersCredentials, graphql: Graphql, faultDownloader: FaultDownloader)
                  (implicit protected val system: ActorSystem,
                            protected val materializer: Materializer,
                            protected val executionContext: ExecutionContext) {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val exceptionHandler = ExceptionHandler {
    case ex =>
      log.error("Exception", ex)
      complete(StatusCodes.InternalServerError, s"Server error: ${ex.getMessage}")
  }

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
            println(ctx)
              mapRejections { rejections => // Prevent browser to invoke basic auth popup.
                rejections.map(_ match {
                  case AuthenticationFailedRejection(cause, challenge) =>
                    val scheme = if (challenge.scheme == "Basic") "x-Basic" else challenge.scheme
                    AuthenticationFailedRejection(cause, HttpChallenge(scheme, challenge.realm, challenge.params))
                  case rejection =>
                    rejection
                })
              } {
                authenticateBasic(realm = "Distribution", authenticate) { case userInfo =>
                  path(graphqlPathPrefix) {
                    seal {
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
                              val context = new GraphqlContext(distributionName, versionHistoryConfig, collections, dir, userInfo)
                              complete(graphql.executeQuery(GraphqlSchema.SchemaDefinition(userInfo.role),
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
                              val context = new GraphqlContext(distributionName, versionHistoryConfig, collections, dir, userInfo)
                              complete(graphql.executeQuery(GraphqlSchema.SchemaDefinition(userInfo.role),
                                context, queryAst, operation, vars))
                            case Failure(error) =>
                              complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
                          }
                        }
                      }
                   }
                } ~ pathPrefix(interactiveGraphqlPathPrefix) {
                  getFromResource("graphiql.html")
                } ~ path(developerVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                  seal {
                    get {
                      authorize(userInfo.role == UserRole.Administrator || userInfo.role == UserRole.Distribution) {
                        getFromFile(dir.getDeveloperVersionImageFile(service, DeveloperDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(userInfo.role == UserRole.Administrator) {
                        path(developerVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                          fileUpload("version-image") {
                            case (fileInfo, byteSource) =>
                              val sink = FileIO.toPath(dir.getDeveloperVersionImageFile(service, DeveloperDistributionVersion.parse(version)).toPath)
                              val future = byteSource.runWith(sink)
                              onSuccess(future) { _ => complete("Complete") }
                          }
                        }
                      }
                    }
                  }
                } ~ path(clientVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                  seal {
                    get {
                      authorize(userInfo.role == UserRole.Administrator || userInfo.role == UserRole.Service) {
                        getFromFile(dir.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(userInfo.role == UserRole.Administrator) {
                        path(clientVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                          fileUpload("version-image") {
                            case (fileInfo, byteSource) =>
                              val sink = FileIO.toPath(dir.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)).toPath)
                              val future = byteSource.runWith(sink)
                              onSuccess(future) { _ => complete("Complete") }
                          }
                        }
                      }
                    }
                  }
                } ~ path(faultReportPath / ".*".r) { faultId =>
                  seal {
                    authorize(userInfo.role == UserRole.Service || userInfo.role == UserRole.Distribution) {
                      val reportDistributionName = if (userInfo.role == UserRole.Distribution) userInfo.name else distributionName
                      fileUpload("fault-report") {
                        case (fileInfo, byteSource) =>
                          faultDownloader.receiveFault(faultId, reportDistributionName, byteSource)
                      }
                    }
                  }
                }
              }
            }
          } ~ get {
            path(browsePath) {
              seal {
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  authorize(userRole == UserRole.Administrator) {
                    browse(None)
                  }
                }
              }
            } ~ pathPrefix(browsePath / ".*".r) { path =>
              seal {
                authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                  authorize(userRole == UserRole.Administrator) {
                    browse(Some(path))
                  }
                }
              }
            } ~ getFromResourceDirectory(uiStaticPathPrefix) ~
            pathPrefix("") {
              getFromResource(uiStaticPathPrefix + "/index.html", ContentType(`text/html`, `UTF-8`))
            }
          }
        }
      }
    }
  }

  private def authenticate(credentials: Credentials): Option[UserInfo] = {
    credentials match {
      case p@Credentials.Provided(userName) =>
        usersCredentials.getCredentials(userName) match {
          case Some(userCredentials) if p.verify(userCredentials.password.hash,
            PasswordHash.generatePasswordHash(_, userCredentials.password.salt)) =>
            Some(UserInfo(userName, userCredentials.role))
          case _ =>
            None
        }
      case _ => None
    }
  }

  private def browse(path: Option[String]): Route = {
    val file = path match {
      case Some(path) =>
        new File(dir.directory, path)
      case None =>
        dir.directory
    }
    if (file.isDirectory) {
      getFromBrowseableDirectory(file.getPath)
    } else {
      getFromFile(file.getPath)
    }
  }

  private def requestLogger(req: HttpRequest): String = {
    "Request: " + req.toString()
  }

  private def resultLogger(res: RouteResult): String = {
    "Result: " + res.toString()
  }
}
