package distribution.client.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UserInfo
import distribution.client.ClientDatabaseCollections
import distribution.client.config.ClientDistributionConfig
import distribution.graphql.GraphqlContext
import sangria.schema._

import scala.concurrent.ExecutionContext

class ClientGraphqlContext(override val config: ClientDistributionConfig,
                            override val dir: ClientDistributionDirectory,
                            override val collections: ClientDatabaseCollections,
                            override val userInfo: UserInfo)
                           (implicit system: ActorSystem, materializer: Materializer,
                            executionContext: ExecutionContext, filesLocker: SmartFilesLocker)
    extends GraphqlContext(config, dir, collections, userInfo)

object ClientGraphQLSchema {
  val QueryType = ObjectType(
    "Query",
    fields[ClientGraphqlContext, Unit](
    )
  )

  val SchemaDefinition = Schema(QueryType)
}