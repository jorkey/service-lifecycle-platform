package distribution.developer.graphql

import com.mongodb.client.model.Filters
import com.vyulabs.update.info.FaultInfo
import distribution.graphql.GraphQLContext
import distribution.graphql.GraphQLSchema._
import sangria.schema._

import collection.JavaConverters._

object DeveloperGraphQLSchema {
  val Client = Argument("client", StringType)
  val Service = Argument("service", StringType)

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
          c.ctx.mongoDb.getCollection[FaultInfo]("faults").find(filters)
        })
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}