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
      ("""{"data":{"distributionConsumersInfo":[{"distributionName":"consumer-distribution","consumerProfile":"common"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          distributionConsumersInfo {
             distributionName,
             consumerProfile
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
      ("""{"data":{"distributionConsumersInfo":[{"distributionName":"consumer-distribution-1","consumerProfile":"profile1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          distributionConsumersInfo {
             distributionName,
             consumerProfile
          }
        }
      """)))
  }

  it should "add/get/remove distribution consumer profiles" in {
    assertResult((OK,
      ("""{"data":{"addDistributionConsumerProfile":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          addDistributionConsumerProfile (
            profile: "consumer-distribution",
            services: [ "service1", "service2", "service3" ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionConsumerProfiles":[{"consumerProfile":"consumer-distribution","services":["service1","service2","service3"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        query {
          distributionConsumerProfiles {
             consumerProfile,
             services
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeDistributionConsumerProfile":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
        mutation {
          removeDistributionConsumerProfile (
            profile: "consumer-distribution"
          )
        }
      """)))

  }
}