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

class DistributionProviderTest extends TestEnvironment {
  behavior of "Distribution providers"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

  it should "add/get/remove distribution providers" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          addDistributionProvider (
            distribution: "provider-distribution",
            url: "http://provider-distribution.com",
            uploadStateInterval: "{ \"length\": 30, \"unit\": \"SECONDS\" }"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distributionName":"provider-distribution","distributionUrl":"http://provider-distribution.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionProvidersInfo {
             distributionName,
             distributionUrl
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
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
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          removeDistributionProvider (
            distribution: "provider-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distributionName":"provider-distribution-1","distributionUrl":"http://provider-distribution-1.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionProvidersInfo {
             distributionName,
             distributionUrl
          }
        }
      """)))
  }
}
