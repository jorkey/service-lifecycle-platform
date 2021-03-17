package com.vyulabs.update.distribution.graphql

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.Materializer
import com.vyulabs.update.common.info.UserInfo
import com.vyulabs.update.common.info.UserRole.UserRole
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

trait UserContext {
  val userInfo: UserInfo

  def authorize[T](roles: UserRole*)(fn: () ⇒ T) = {
    if (!roles.isEmpty && !roles.contains(userInfo.role)) {
      throw AuthorizationException("You do not have permission to do this operation")
    }
    fn()
  }
}

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
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }

  def executeSubscriptionQuery(schema: Schema[GraphqlContext, Unit], context: GraphqlContext,
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
        case error: QueryAnalysisError => ToResponseMarshallable(BadRequest -> error.resolveError)
        case error: ErrorWithResolver => ToResponseMarshallable(InternalServerError -> error.resolveError)
      }
  }
}