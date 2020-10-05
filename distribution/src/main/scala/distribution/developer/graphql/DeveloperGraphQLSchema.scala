package distribution.developer.graphql

import com.vyulabs.update.info.FaultInfo

import sangria.schema._
import distribution.graphql.GraphQLContext
import distribution.graphql.GraphQLSchema._

object DeveloperGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("getAll", ListType(FaultInfoType), resolve = c =>
        c.ctx.mongoDb.getCollection[FaultInfo]("qwerty").find(null))
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}