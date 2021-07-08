package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.model.{ContentType, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.info.{AccessToken, UserRole}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlContext, GraphqlHttpSupport, GraphqlSchema, GraphqlWebSocketSupport, GraphqlWorkspace}
import org.slf4j.LoggerFactory
import sangria.ast.OperationType
import sangria.parser.QueryParser
import spray.json._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class Distribution(val workspace: GraphqlWorkspace, val graphql: Graphql)
                  (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)
    extends GraphqlHttpSupport with GraphqlWebSocketSupport {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val exceptionHandler = ExceptionHandler {
    case ex =>
      log.error("Exception", ex)
      complete(StatusCodes.InternalServerError, s"Server error: ${ex.toString}")
  }

  val route: Route = {
    logRequest(requestLogger _) {
      logResult(resultLogger _) {
        handleExceptions(exceptionHandler) {
          extractRequestContext { ctx =>
            pathPrefix(graphqlPathPrefix) {
              seal {
                (workspace.getOptionalAccessToken() & optionalHeaderValueByName("X-Apollo-Tracing")) { (token, tracing) =>
                  post {
                    entity(as[JsValue]) { requestJson =>
                      executeGraphqlRequest(token, requestJson, tracing.isDefined)
                    }
                  } ~ get {
                    handleWebSocketMessagesForProtocol(
                      handleWebSocket(token, tracing.isDefined), "graphql-transport-ws")
                  }
                }
              }
            } ~ pathPrefix(loadPathPrefix) {
              seal {
                workspace.getAccessToken()(log) { case token =>
                  path(developerVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                    get {
                      authorize(token.hasRole(UserRole.Builder) || token.hasRole(UserRole.Distribution)) {
                        getFromFile(workspace.directory.getDeveloperVersionImageFile(service,
                          DeveloperDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(token.hasRole(UserRole.Builder)) {
                        fileUpload(imageField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getDeveloperVersionImageFile(service, DeveloperDistributionVersion.parse(version)).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(clientVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                    get {
                      authorize(token.hasRole(UserRole.Builder) || token.hasRole(UserRole.Updater)) {
                        getFromFile(workspace.directory.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(token.hasRole(UserRole.Builder)) {
                        fileUpload(imageField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(faultReportPath / ".*".r) { faultId =>
                    get {
                      authorize(token.hasRole(UserRole.Developer) || token.hasRole(UserRole.Administrator)) {
                        getFromFile(workspace.directory.getFaultReportFile(faultId))
                      }
                    } ~ post {
                      authorize(token.hasRole(UserRole.Updater) || token.hasRole(UserRole.Distribution)) {
                        fileUpload(faultReportPath) {
                          case (fileInfo, byteSource) =>
                            log.info(s"Receive fault report file from client ${workspace.config.distribution}")
                            val file = workspace.directory.getFaultReportFile(faultId)
                            val sink = FileIO.toPath(file.toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
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

  private def requestLogger(req: HttpRequest): String = "Request: " + req.toString()

  private def resultLogger(res: RouteResult): String = "Result: " + res.toString()
}
