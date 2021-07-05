package com.vyulabs.update.distribution.graphql

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError, _}
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import sangria.slowlog.SlowLog
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class AuthenticationException(msg: String) extends Exception(msg)
case class AuthorizationException(msg: String) extends Exception(msg)
case class NotFoundException(msg: String = "Not found") extends Exception(msg)
case class InvalidConfigException(msg: String) extends Exception(msg)

class Graphql() extends SprayJsonSupport {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  val errorHandler = ExceptionHandler(
    onException = {
      case (m, e: Exception) ⇒ {
        log.error("Exception", e)
        HandledException(e.getMessage)
      }
    }
  )

  def executeQuery(schema: Schema[GraphqlContext, Unit], context: GraphqlContext,
                    query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty,
                    tracing: Boolean = false)
                   (implicit executionContext: ExecutionContext): Future[(StatusCode, JsValue)] = {
    Executor.execute(schema = schema, queryAst = query, userContext = context, operationName = operation,
      variables = variables, exceptionHandler = errorHandler,
      middleware = AuthMiddleware :: (if (tracing) SlowLog.apolloTracing :: Nil else Nil)
    )
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError =>
          log.error("Graphql query error", error)
          BadRequest -> error.resolveError
        case error: ErrorWithResolver =>
          log.error("Graphql query error", error)
          InternalServerError -> error.resolveError
      }
  }

  def executeSubscriptionQueryToSSE(schema: Schema[GraphqlContext, Unit], context: GraphqlContext,
                                    query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty)
                                   (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext): Future[ToResponseMarshallable] = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    import sangria.execution.ExecutionScheme.Stream
    import sangria.streaming.akkaStreams._
    val executor = Executor(schema, exceptionHandler = errorHandler)
    executor.prepare(query, context, (), operation, variables)
      .map { preparedQuery =>
        ToResponseMarshallable(preparedQuery.execute()
          .map(result ⇒ ServerSentEvent(result.compactPrint))
          .recover { case NonFatal(ex) =>
            log.error("Unexpected error during event stream processing", ex)
            ServerSentEvent(ex.getMessage)
          })
      }
      .recover {
        case error: QueryAnalysisError =>
          log.error("Graphql query error", error)
          ToResponseMarshallable(BadRequest -> error.resolveError)
        case error: ErrorWithResolver =>
          log.error("Graphql query error", error)
          ToResponseMarshallable(InternalServerError -> error.resolveError)
      }
  }

  def executeSubscriptionQueryToJsonSource(schema: Schema[GraphqlContext, Unit], context: GraphqlContext,
                                           query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty,
                                           tracing: Boolean = false)
                                           (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)
      : Source[String, NotUsed] = {
    import sangria.execution.ExecutionScheme.Stream
    import sangria.streaming.akkaStreams._
    Executor.execute(schema = schema, queryAst = query, userContext = context, operationName = operation,
      variables = variables, exceptionHandler = errorHandler,
      middleware = AuthMiddleware :: (if (tracing) SlowLog.apolloTracing :: Nil else Nil)
    ).map(_.compactPrint)
  }
}