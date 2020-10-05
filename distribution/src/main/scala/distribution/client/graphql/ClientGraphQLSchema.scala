package distribution.client.graphql

import distribution.graphql.GraphQLContext
import sangria.schema._

object ClientGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
    )
  )

  val SchemaDefinition = Schema(QueryType)
}