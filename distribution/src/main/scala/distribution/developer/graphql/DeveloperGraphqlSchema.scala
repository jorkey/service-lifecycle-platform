package distribution.developer.graphql

import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.FaultInfo
import distribution.graphql.GraphqlContext
import distribution.graphql.GraphqlSchema._
import sangria.schema._

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext}

object DeveloperGraphqlSchema {
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  val Client = Argument("clientName", StringType)
  val Service = Argument("serviceName", StringType)
  val Last = Argument("last", IntType)

  val QueryType = ObjectType(
    "Query",
    fields[GraphqlContext, Unit](
      Field("faults", ListType(FaultInfoType),
        description = Some("Returns a list of fault reports."),
        arguments = Client :: Service :: Nil,
        resolve = c => {
          val clientArg = c.argOpt(Client).map { client => Filters.eq("client", client) }
          val serviceArg = c.argOpt(Service).map { service => Filters.eq("service", service) }
          val filters = Filters.and((clientArg ++ serviceArg).asJava)
          // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
          val sort = c.argOpt(Last).map { last => Sorts.descending("_id") }
          for {
            collection <- c.ctx.mongoDb.getCollection[FaultInfo]("faults")
            faults <- collection.find(filters, sort, c.argOpt(Last))
          } yield faults
        })
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}