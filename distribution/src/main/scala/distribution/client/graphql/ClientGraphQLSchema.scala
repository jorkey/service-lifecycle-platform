package distribution.client.graphql

import distribution.graphql.GraphqlContext
import sangria.schema._

object ClientGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[GraphqlContext, Unit](
    )
  )

  val SchemaDefinition = Schema(QueryType)
}