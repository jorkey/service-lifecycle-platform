package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentType, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.info.AccountRole
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.distribution.graphql.{Graphql, GraphqlHttpSupport, GraphqlWebSocketSupport, GraphqlWorkspace}
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.ExecutionContext

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
                    pathPrefix(websocketPathPrefix) {
                      seal {
                        handleWebSocketMessagesForProtocol(
                          handleWebSocket(tracing.isDefined), "graphql-transport-ws")
                      }
                    } ~ {
                      parameters("query", "operation".?, "variables".?) { (query, operation, vars) =>
                        val variables = vars.map(_.parseJson.asJsObject).getOrElse(JsObject.empty)
                        executeGraphqlRequest(token, query, operation, variables, tracing.isDefined)
                      }
                    }
                  }
                }
              }
            } ~ pathPrefix(loadPathPrefix) {
              seal {
                workspace.getAccountRole()(log) { case role =>
                  path(developerVersionImagePath / ".*".r / ".*".r) { (service, version) =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder || role == AccountRole.DistributionConsumer) {
                        getFromFile(workspace.directory.getDeveloperVersionImageFile(service,
                          DeveloperDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
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
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder || role ==  AccountRole.Updater) {
                        getFromFile(workspace.directory.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
                        fileUpload(imageField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getClientVersionImageFile(service, ClientDistributionVersion.parse(version)).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(developerPrivateFilePath / ".*".r / ".*".r) { (service, path) =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
                        getFromFile(workspace.directory.getDeveloperPrivateFile(
                          workspace.config.distribution, service, path))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator) {
                        fileUpload(fileField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getDeveloperPrivateFile(
                              workspace.config.distribution, service, path).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(clientPrivateFilePath / ".*".r / ".*".r) { (service, path) =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
                        getFromFile(workspace.directory.getClientPrivateFile(
                          workspace.config.distribution, service, path))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator) {
                        fileUpload(fileField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getClientPrivateFile(
                              workspace.config.distribution, service, path).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(faultReportPath / ".*".r) { id =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Developer) {
                        getFromFile(workspace.directory.getFaultReportFile(id))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Updater || role == AccountRole.DistributionConsumer) {
                        fileUpload(faultReportPath) {
                          case (fileInfo, byteSource) =>
                            log.info(s"Receive fault report file from client ${workspace.config.distribution}")
                            val file = workspace.directory.getFaultReportFile(id)
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

  private def requestLogger(req: HttpRequest): String = "Request: " + req.toString()

  private def resultLogger(res: RouteResult): String = "Result: " + res.toString()
}
