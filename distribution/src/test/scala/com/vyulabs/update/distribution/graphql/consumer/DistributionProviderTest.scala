package com.vyulabs.update.distribution.graphql.consumer

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DistributionProviderTest extends TestEnvironment {
  behavior of "Distribution providers"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  it should "add/get/remove distribution providers" in {
    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          addDistributionProvider (
            distribution: "provider-distribution",
            url: "http://provider-distribution.com",
            uploadStateInterval: "{ \"length\": 30, \"unit\": \"SECONDS\" }"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distribution":"provider-distribution","distributionUrl":"http://provider-distribution.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          distributionProvidersInfo {
             distribution,
             distributionUrl
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          addDistributionProvider (
            distribution: "provider-distribution-1",
            url: "http://provider-distribution-1.com",
            uploadStateInterval: "{ \"length\": 20, \"unit\": \"SECONDS\" }"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          removeDistributionProvider (
            distribution: "provider-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distribution":"provider-distribution-1","distributionUrl":"http://provider-distribution-1.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          distributionProvidersInfo {
             distribution,
             distributionUrl
          }
        }
      """)))
  }
}
