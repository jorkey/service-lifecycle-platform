package distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.headers.HttpChallenge
import akka.http.scaladsl.model.{ContentType, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Route}
import akka.stream.Materializer
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole, UsersCredentials}
import distribution.config.DistributionConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.uploaders.DeveloperFaultUploader
import distribution.utils._
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

import com.vyulabs.update.distribution.DistributionWebPaths._

class DeveloperDistribution(protected val dir: DistributionDirectory,
                            protected val collections: DatabaseCollections,
                            protected val config: DistributionConfig,
                            protected val usersCredentials: UsersCredentials,
                            protected val graphql: Graphql,
                            protected val faultUploader: DeveloperFaultUploader)
                           (implicit protected val system: ActorSystem,
                            protected val materializer: Materializer,
                            protected val executionContext: ExecutionContext,
                            protected val filesLocker: SmartFilesLocker)
       extends Distribution(usersCredentials, graphql) with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils with SprayJsonSupport {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()

  protected val versionHistoryConfig = config.versionHistory

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
                            val context = new GraphqlContext(config.versionHistory, dir, collections, userInfo)
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
                            val context = new GraphqlContext(config.versionHistory, dir, collections, userInfo)
                            complete(graphql.executeQuery(GraphqlSchema.SchemaDefinition(userInfo.role),
                              context, queryAst, operation, vars))
                          case Failure(error) =>
                            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
                        }
                      }
                    }
                  }
                }
              }
            } ~
            pathPrefix(interactiveGraphqlPathPrefix) {
              getFromResource("graphiql.html")
            } /* TODO graphql ~
            pathPrefix(apiPathPrefix) {
                    authenticateBasic(realm = "Distribution", authenticate) { case UserInfo(userName, userRole) =>
                      get {
                          path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                            getFromFile(dir.getVersionImageFile(service, BuildVersion.parse(version)))
                          } ~
                      } ~
                        post {
                          authorize(userRole == UserRole.Administrator) {
                            path(versionImagePath / ".*".r / ".*".r) { (service, version) =>
                              val buildVersion = BuildVersion.parse(version)
                              versionImageUpload(service, buildVersion)
                            } ~
                          } ~
                        }
                    }
                }
              }
            }
            } ~ */
          }
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
