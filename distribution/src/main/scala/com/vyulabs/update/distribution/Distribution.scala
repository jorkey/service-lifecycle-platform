package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.{ContentType, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{AuthenticationFailedRejection, ExceptionHandler, Route, RouteResult}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.graphql.Graphql
import com.vyulabs.update.distribution.users.UsersCredentials
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema, GraphqlWorkspace}
import com.vyulabs.update.distribution.users.{PasswordHash, UsersCredentials}
import org.slf4j.LoggerFactory

class Distribution(workspace: GraphqlWorkspace, usersCredentials: UsersCredentials, graphql: Graphql)
                  (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val exceptionHandler = ExceptionHandler {
    case ex =>
      log.error("Exception", ex)
      complete(StatusCodes.InternalServerError, s"Server error: ${ex.toString}")
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
              mapRejections { rejections => // Prevent browser to invoke basic auth popup.
                rejections.map(_ match {
                  case AuthenticationFailedRejection(cause, challenge) =>
                    val scheme = if (challenge.scheme == "Basic") "x-Basic" else challenge.scheme
                    AuthenticationFailedRejection(cause, HttpChallenge(scheme, challenge.realm, challenge.params))
                  case rejection =>
                    rejection
                })
              } {
                pathPrefix(graphqlPathPrefix) {
                  seal {
                    authenticateBasic(realm = "Distribution", authenticate) { case userInfo =>
                      post {
                        entity(as[JsValue]) { requestJson =>
                          val JsObject(fields) = requestJson
                          val JsString(query) = fields("query")
                          val operation = fields.get("operation") collect { case JsString(op) => op }
                          val variables = fields.get("variables").map(_.asJsObject).getOrElse(JsObject.empty)
                          executeGraphqlRequest(userInfo, workspace, query, operation, variables) }
                      } ~ get {
                        parameters("query", "operation".?, "variables".?) { (query, operation, vars) =>
                          val variables = vars.map(_.parseJson.asJsObject).getOrElse(JsObject.empty)
                          executeGraphqlRequest(userInfo, workspace, query, operation, variables) }
                      }
                    }
                  }
                } ~ pathPrefix(interactiveGraphqlPathPrefix) {
                  getFromResource("graphiql.html")
                } ~ pathPrefix(loadPathPrefix) {
                  seal {
                    authenticateBasic(realm = "Distribution", authenticate) { case userInfo =>
                      path(developerVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                        get {
                          authorize(userInfo.role == UserRole.Administrator || userInfo.role == UserRole.Distribution) {
                            getFromFile(workspace.dir.getDeveloperVersionImageFile(service,
                              DeveloperDistributionVersion.parse(version)))
                          }
                        } ~ post {
                          authorize(userInfo.role == UserRole.Administrator) {
                            fileUpload(versionImageField) {
                              case (fileInfo, byteSource) =>
                                val sink = FileIO.toPath(workspace.dir.getDeveloperVersionImageFile(service, DeveloperDistributionVersion.parse(version)).toPath)
                                val future = byteSource.runWith(sink)
                                onSuccess(future) { _ => complete(OK) }
                            }
                          }
                        }
                      } ~ path(clientVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                        get {
                          authorize(userInfo.role == UserRole.Administrator || userInfo.role == UserRole.Service) {
                            getFromFile(workspace.dir.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)))
                          }
                        } ~ post {
                          authorize(userInfo.role == UserRole.Administrator) {
                            fileUpload(versionImageField) {
                              case (fileInfo, byteSource) =>
                                val sink = FileIO.toPath(workspace.dir.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)).toPath)
                                val future = byteSource.runWith(sink)
                                onSuccess(future) { _ => complete(OK) }
                            }
                          }
                        }
                      } ~ path(faultReportPath / ".*".r) { faultId =>
                        get {
                          authorize(userInfo.role == UserRole.Administrator) {
                            getFromFile(workspace.dir.getFaultReportFile(faultId))
                          }
                        } ~ post {
                          authorize(userInfo.role == UserRole.Service || userInfo.role == UserRole.Distribution) {
                            fileUpload("fault-report") {
                              case (fileInfo, byteSource) =>
                                log.info(s"Receive fault report file from client ${workspace.distributionName}")
                                val file = workspace.dir.getFaultReportFile(faultId)
                                val sink = FileIO.toPath(file.toPath)
                                val future = byteSource.runWith(sink)
                                onSuccess(future) { _ => complete(OK) }
                            }
                          }
                        }
                      }
                    }
                  }
                } ~ get {
                  getFromResourceDirectory(uiStaticPathPrefix) ~
                    pathPrefix("") {
                      getFromResource(uiStaticPathPrefix + "/index.html", ContentType(`text/html`, `UTF-8`))
                    }
                }
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

  private def executeGraphqlRequest(userInfo: UserInfo, workspace: GraphqlWorkspace,
                                    query: String, operation: Option[String], variables: JsObject): Route = {
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val context = new GraphqlContext(userInfo, workspace)
        log.debug(s"Execute graphql query ${query}, operation ${operation}, variables ${variables}")
        complete(graphql.executeQuery(GraphqlSchema.SchemaDefinition(userInfo.role),
            context, queryAst, operation, variables).andThen {
          case Success((statusCode, value)) =>
            log.debug(s"Graphql query terminated with status ${statusCode}, value ${value}")
          case Failure(ex) =>
        })
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.toString)))
    }
  }

  private def requestLogger(req: HttpRequest): String = {
    "Request: " + req.toString()
  }

  private def resultLogger(res: RouteResult): String = {
    "Result: " + res.toString()
  }
}
