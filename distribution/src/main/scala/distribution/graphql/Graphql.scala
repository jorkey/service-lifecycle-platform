package distribution.graphql

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.sprayJson._
import sangria.parser.QueryParser
import sangria.schema.Schema
import spray.json.{JsObject, JsString, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class GraphqlContext(mongoDb: MongoDb)

class Graphql(schema: Schema[GraphqlContext, Unit], context: GraphqlContext) extends SprayJsonSupport {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  def endpoint(requestJSON: JsValue)(implicit ec: ExecutionContext): Route = {

    val JsObject(fields) = requestJSON
    val JsString(query) = fields("query")

    QueryParser.parse(query) match {
      case Success(queryAst) =>
        val operation = fields.get("operationName") collect {
          case JsString(op) => op
        }
        val variables = fields.get("variables") match {
          case Some(obj: JsObject) => obj
          case _ => JsObject.empty
        }
        complete(executeQuery(queryAst, operation, variables))
      case Failure(error) =>
        complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
    }
  }

  def executeQuery(query: Document, operation: Option[String], vars: JsObject)
                  (implicit executionContext: ExecutionContext): Future[(StatusCode, JsValue)] = {
    Executor.execute(
      schema,
      query,
      context,
      variables = vars,
      operationName = operation
    )
      .map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}