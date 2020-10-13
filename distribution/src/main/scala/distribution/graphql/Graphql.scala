package distribution.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import distribution.GraphqlContext
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import spray.json.{JsObject, JsValue}

import scala.concurrent.{ExecutionContext, Future}

class Graphql[Context <: GraphqlContext](schema: Schema[Context, Unit], context: Context) extends SprayJsonSupport {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  def executeQuery(query: Document, operation: Option[String] = None, variables: JsObject = JsObject.empty)
                  (implicit executionContext: ExecutionContext): Future[(StatusCode, JsValue)] = {
    Executor.execute(
      schema = schema,
      queryAst = query,
      userContext = context,
      variables = variables,
      operationName = operation
    )
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}