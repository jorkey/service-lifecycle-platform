package distribution.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole.UserRole
import distribution.DatabaseCollections
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, ExceptionHandler, Executor, HandledException, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}

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

trait GraphqlContext extends UserContext {
  val dir: DistributionDirectory
  val collections: DatabaseCollections
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

  def executeQuery[Context](schema: Schema[Context, Unit], context: Context,
                            query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty)
                           (implicit executionContext: ExecutionContext): Future[(StatusCode, JsValue)] = {
    Executor.execute(
      schema = schema,
      queryAst = query,
      userContext = context,
      operationName = operation,
      variables = variables,
      exceptionHandler = errorHandler
    )
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}