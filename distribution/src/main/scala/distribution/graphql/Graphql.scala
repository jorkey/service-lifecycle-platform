package distribution.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK, Unauthorized}
import com.vyulabs.update.common.Common.UserName
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole.UserRole
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.Executor.ExceptionHandler
import sangria.execution.{ErrorWithResolver, ExceptionHandler, Executor, HandledException, QueryAnalysisError}
import sangria.marshalling.ResultMarshaller
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}

case class AuthorizationException(msg: String) extends Exception(msg)

trait UserContext {
  val userInfo: Option[UserInfo]

  def authorize[T](roles: UserRole*)(fn: () ⇒ T) = {
    (userInfo, roles) match {
      case (_, Seq()) =>
      case (None, _) =>
        throw AuthorizationException("You are not logged in")
      case (Some(userInfo), _) =>
        if (roles.contains(userInfo.role)) {
          throw AuthorizationException("You do not have permission to do this operation")
        }
    }
  }
}

trait GraphqlContext extends UserContext {
  val dir: DistributionDirectory
  val mongoDb: MongoDb
}

class Graphql[Context <: GraphqlContext](schema: Schema[Context, Unit]) extends SprayJsonSupport {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  val errorHandler = ExceptionHandler(
    onException = {
      case (m, AuthorizationException(message)) ⇒ HandledException(message)
    }
  )

  def executeQuery(context: Context, query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty)
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