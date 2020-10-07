package distribution.developer.graphql

import com.mongodb.client.model.{Filters, Projections, Sorts}
import com.vyulabs.update.info.FaultInfo
import distribution.graphql.GraphQLContext
import distribution.graphql.GraphQLSchema._
import sangria.schema._

import collection.JavaConverters._

object DeveloperGraphQLSchema {
  val Client = Argument("client", StringType)
  val Service = Argument("service", StringType)
  val Last = Argument("last", IntType)

  val QueryType = ObjectType(
    "Query",
    fields[GraphQLContext, Unit](
      Field("faults", ListType(FaultInfoType),
        description = Some("Returns a list of fault reports."),
        arguments = Client :: Service :: Nil,
        resolve = c => {
          val clientArg = c.argOpt(Client).map { client => Filters.eq("client", client) }
          val serviceArg = c.argOpt(Service).map { service => Filters.eq("service", service) }
          val filters = Filters.and((clientArg ++ serviceArg).asJava)
          // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
          val sort = c.argOpt(Last).map { last => Sorts.descending("_id") }
          c.ctx.mongoDb.getCollection[FaultInfo]("faults").find(filters, sort, c.argOpt(Last))
        })
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}