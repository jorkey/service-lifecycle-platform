package distribution.client.graphql

import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import distribution.GraphqlContext
import distribution.mongo.MongoDb
import sangria.schema._

case class ClientGraphqlContext(dir: ClientDistributionDirectory, mongoDb: MongoDb) extends GraphqlContext

object ClientGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[ClientGraphqlContext, Unit](
    )
  )

  val SchemaDefinition = Schema(QueryType)
}