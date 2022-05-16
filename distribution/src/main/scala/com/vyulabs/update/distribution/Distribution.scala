package com.vyulabs.update.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Directives.{path, pathPrefix, _}
import akka.http.scaladsl.server.Route._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.{ExceptionHandler, Route, RouteResult}
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.info.AccountRole
import com.vyulabs.update.common.logs.LogFormat
import com.vyulabs.update.common.utils.Utils
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
                  } ~ path(developerPrivateFilePath / ".*".r) { path =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
                        getFromFile(workspace.directory.getDeveloperPrivateFile(path))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator) {
                        fileUpload(fileField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getDeveloperPrivateFile(path).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(clientPrivateFilePath / ".*".r) { path =>
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Builder) {
                        getFromFile(workspace.directory.getClientPrivateFile(path))
                      }
                    } ~ post {
                      authorize(role == AccountRole.Administrator) {
                        fileUpload(fileField) {
                          case (fileInfo, byteSource) =>
                            val sink = FileIO.toPath(workspace.directory.getClientPrivateFile(path).toPath)
                            val future = byteSource.runWith(sink)
                            onSuccess(future) { _ => complete(OK) }
                        }
                      }
                    }
                  } ~ path(logsPath) {
                    get {
                      authorize(role == AccountRole.Administrator || role == AccountRole.Developer) {
                        parameters("service".as[ServiceId].optional, "instance".as[InstanceId].optional,
                                    "directory".as[ServiceDirectory].optional, "process".as[ProcessId].optional,
                                    "task".as[TaskId].optional, "levels".optional, "find".optional,
                                    "fromTime".optional, "toTime".optional,
                                    "from".as[Long].optional, "to".as[Long].optional,
                                    "limit".as[Int].optional) {
                          (service, instance, directory, process, task, levels, find, fromTime, toTime, from, to, limit) =>
                            complete(HttpEntity.Chunked(`application/octet-stream`,
                              workspace.getLogsToStream(service, instance, directory, process, task,
                                                        levels.map(_.split(':')), find,
                                                        fromTime.map(Utils.parseISO8601Date(_).get), toTime.map(Utils.parseISO8601Date(_).get),
                                                        from, to, limit).map(log => {
                                (LogFormat.serialize(log, service.isEmpty, instance.isEmpty, directory.isEmpty, process.isEmpty,
                                  service.isEmpty && task.isEmpty) + "\n").getBytes("utf8") })))
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
