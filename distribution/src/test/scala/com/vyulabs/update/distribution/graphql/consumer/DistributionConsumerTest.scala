package com.vyulabs.update.distribution.graphql.consumer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DistributionConsumerTest extends TestEnvironment {
  behavior of "Distribution consumers"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace();
    log.error("Uncatched exception", ex)
  })

  it should "add/get/remove distribution consumers" in {
    assertResult((OK,
      ("""{"data":{"addDistributionConsumer":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addDistributionConsumer (
            distribution: "consumer-distribution",
            profile: "common",
            testDistributionMatch: "test-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionConsumersInfo":[{"distribution":"consumer-distribution","servicesProfile":"common"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          distributionConsumersInfo {
             distribution,
             servicesProfile
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"addDistributionConsumer":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addDistributionConsumer (
            distribution: "consumer-distribution-1",
            profile: "profile1"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeDistributionConsumer":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          removeDistributionConsumer (
            distribution: "consumer-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionConsumersInfo":[{"distribution":"consumer-distribution-1","servicesProfile":"profile1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          distributionConsumersInfo {
             distribution,
             servicesProfile
          }
        }
      """)))
  }
}
