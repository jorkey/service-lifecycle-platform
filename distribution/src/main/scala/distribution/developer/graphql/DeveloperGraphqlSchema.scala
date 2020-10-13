package distribution.developer.graphql

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.info.ClientFaultReport
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.GraphqlContext
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.utils.{ClientsUtils, StateUtils}
import distribution.graphql.GraphqlSchema._
import distribution.mongo.MongoDb
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}

import collection.JavaConverters._
import scala.concurrent.ExecutionContext
import sangria.schema._

case class DeveloperGraphqlContext(config: DeveloperDistributionConfig, dir: DeveloperDistributionDirectory, mongoDb: MongoDb)
                                  (implicit protected val system: ActorSystem,
                                   protected val materializer: Materializer,
                                   protected val executionContext: ExecutionContext,
                                   protected val filesLocker: SmartFilesLocker) extends GraphqlContext
  with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils {}

object DeveloperGraphqlSchema {
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  val Client = Argument("clientName", OptionInputType(StringType))
  val Service = Argument("serviceName", OptionInputType(StringType))
  val Last = Argument("last", OptionInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[DeveloperGraphqlContext, Unit](
      /*Field("user", fieldType = UserInfoType,
        resolve = c => {
          c.ctx.dir.
        }

        ),*/
      Field("clients", ListType(ClientInfoType),
        resolve = c => c.ctx.getClientsInfo()),
      /*Field("instanceVersions", ListType(InstanceVersionsType),
        arguments = Client :: Nil,
        resolve = c => {
          c.ctx.getClientInstanceVersions("")
        }),*/
      Field("faults", ListType(ClientFaultReportType),
        arguments = Client :: Service :: Last :: Nil,
        resolve = c => {
          val clientArg = c.arg(Client).map { client => Filters.eq("clientName", client) }
          val serviceArg = c.arg(Service).map { service => Filters.eq("serviceName", service) }
          val filters = Filters.and((clientArg ++ serviceArg).asJava)
          // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
          val sort = c.arg(Last).map { last => Sorts.descending("_id") }
          for {
            collection <- c.ctx.mongoDb.getCollection[ClientFaultReport]("faults")
            faults <- collection.find(filters, sort, c.arg(Last))
          } yield faults
        })
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}