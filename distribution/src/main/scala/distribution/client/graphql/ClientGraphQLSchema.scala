package distribution.client.graphql

import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import com.vyulabs.update.users.UserInfo
import distribution.client.ClientDatabaseCollections
import distribution.graphql.GraphqlContext
import distribution.mongo.MongoDb
import sangria.schema._

case class ClientGraphqlContext(dir: ClientDistributionDirectory, collections: ClientDatabaseCollections, userInfo: UserInfo) extends GraphqlContext

object ClientGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[ClientGraphqlContext, Unit](
    )
  )

  val SchemaDefinition = Schema(QueryType)
}