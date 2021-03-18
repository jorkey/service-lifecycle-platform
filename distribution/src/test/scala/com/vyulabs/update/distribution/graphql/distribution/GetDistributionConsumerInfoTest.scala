package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{AccessToken, DistributionConsumerInfo, UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetDistributionConsumerInfoTest extends TestEnvironment {
  behavior of "Config Client Request"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("distribution1", "common", Some("test"))))
  }

  it should "get distribution consumer info" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("distribution1", Seq(UserRole.Distribution))), workspace)

    assertResult((OK,
      ("""{"data":{"distributionConsumerInfo":{"consumerProfile":"common","testDistributionMatch":"test"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionConsumerInfo {
            consumerProfile,
            testDistributionMatch
          }
        }
      """)))
  }
}
