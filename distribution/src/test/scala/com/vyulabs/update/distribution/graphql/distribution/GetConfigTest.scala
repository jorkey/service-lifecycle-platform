package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetConfigTest extends TestEnvironment {
  behavior of "Config Client Request"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Developer_DistributionClientsInfo.insert(DistributionClientInfo("distribution1", DistributionClientConfig("common", Some("test")))))
  }

  it should "get config for client" in {
    val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

    assertResult((OK,
      ("""{"data":{"distributionClientConfig":{"installProfile":"common","testDistributionMatch":"test"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionClientConfig {
            installProfile,
            testDistributionMatch
          }
        }
      """)))
  }
}
