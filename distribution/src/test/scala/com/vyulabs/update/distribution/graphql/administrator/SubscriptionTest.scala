package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class SubscriptionTest extends TestEnvironment {
  behavior of "Subscriptions"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  it should "get subscription" in {
    assertResult((OK,
      ("""{"data":{"testSubscription":"line"}}""").parseJson))(
      result(graphql.executeSubscriptionQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        subscription {
          testSubscription
        }
      """))
    )
  }
}
