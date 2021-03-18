package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{AccessToken, UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DistributionConsumerTest extends TestEnvironment {
  behavior of "Distribution providers"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace();
    log.error("Uncatched exception", ex)
  })

  val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

  it should "add/get/remove distribution consumers" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

    assertResult((OK,
      ("""{"data":{"addDistributionConsumer":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          removeDistributionConsumer (
            distribution: "consumer-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionConsumersInfo":[{"distributionName":"consumer-distribution-1","consumerProfile":"profile1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        mutation {
          removeDistributionConsumerProfile (
            profile: "consumer-distribution"
          )
        }
      """)))

  }
}
