package distribution.developer.graphql

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.info.FaultInfo
import com.vyulabs.update.info.FaultInfo._
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory
import sangria.ast.Document
import sangria.execution.Executor
import sangria.schema.ListType
import sangria.schema._
import sangria.macros.derive._
import sangria.schema.ObjectType
import spray.json.JsObject

import scala.concurrent.ExecutionContext

private case class GraphQLContext(mongoDb: MongoDb)

object GraphQLSchema {
  val FaultInfoType = deriveObjectType[GraphQLContext, FaultInfo]()

  val QueryType = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("getAll", ListType(FaultInfoType), resolve = c =>
        c.ctx.mongoDb.getCollection[FaultInfo]("qwerty").find(null))
    )
  )

  val SchemaDefinition = Schema(QueryType)
}

class test() {
  protected implicit val log = LoggerFactory.getLogger(this.getClass)

  private val mongoDb = new MongoDb("")

  private def executeGraphQLQuery(query: Document, operation: Option[String], vars: JsObject)
                                 (implicit executionContext: ExecutionContext) = {
    val l = Executor.execute(
      GraphQLSchema.SchemaDefinition,
      query,
      GraphQLContext(mongoDb),
      variables = vars,
      operationName = operation
    ).map(OK -> _)
      .recover {
        case error: QueryAnalysisError => BadRequest -> error.resolveError
        case error: ErrorWithResolver => InternalServerError -> error.resolveError
      }
  }
}