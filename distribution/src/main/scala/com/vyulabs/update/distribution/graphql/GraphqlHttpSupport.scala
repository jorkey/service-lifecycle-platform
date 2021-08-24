package com.vyulabs.update.distribution.graphql

import akka.http.scaladsl.server.Directives.complete
import org.slf4j.Logger
import spray.json.{JsObject, JsString, JsValue}

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes.{BadRequest, OK}
import akka.http.scaladsl.server.{Route}
import akka.stream.Materializer
import com.vyulabs.update.common.info.{AccessToken}
import sangria.ast.OperationType
import sangria.parser.QueryParser

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait GraphqlHttpSupport {
  protected val workspace: GraphqlWorkspace
  protected val graphql: Graphql

  def executeGraphqlRequest(token: Option[AccessToken], requestJson: JsValue, tracing: Boolean)
                           (implicit system: ActorSystem, materializer: Materializer,
                            executionContext: ExecutionContext, log: Logger): Route = {
    val JsObject(fields) = requestJson
    val JsString(query) = fields("query")
    val operation = fields.get("operation") collect { case JsString(op) => op }
    val variables = fields.get("variables").map(_.asJsObject).getOrElse(JsObject.empty)
    executeGraphqlRequest(token, query, operation, variables, tracing)
  }

  def executeGraphqlRequest(token: Option[AccessToken], query: String, operation: Option[String], variables: JsObject, tracing: Boolean)
                           (implicit system: ActorSystem, materializer: Materializer,
                            executionContext: ExecutionContext, log: Logger): Route = {
    QueryParser.parse(query) match {
      case Success(document) =>
        val context = GraphqlContext(token, None, workspace)
        log.debug(s"Execute graphql ${query}, operation ${operation}, variables ${variables}")
        document.operationType(operation) match {
          case Some(OperationType.Subscription) =>
            complete(graphql.executeSubscriptionQueryToSSE(GraphqlSchema.SchemaDefinition, context, document, operation, variables))
          case _ =>
            complete(graphql.executeQuery(GraphqlSchema.SchemaDefinition, context, document, operation, variables, tracing).andThen {
              case Success((statusCode, value)) =>
                log.debug(s"Graphql query terminated with status ${statusCode}, value ${value}")
              case Failure(ex) =>
                log.error(s"Graphql query terminated with error", ex)
            })
        }
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.toString)))
    }
  }
}
