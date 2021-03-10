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

class DistributionProviderTest extends TestEnvironment {
  behavior of "Distribution providers"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  val graphqlContext = GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  it should "add/get distribution providers" in {
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          addDistributionProvider (
            distribution: "consumer-distribution",
            url: "http://consumer-distribution.com",
            uploadStateInterval: "{ \"length\": 30, \"unit\": \"SECONDS\" }"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distributionName":"consumer-distribution","distributionUrl":"http://consumer-distribution.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionProvidersInfo {
             distributionName,
             distributionUrl
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"addDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          addDistributionProvider (
            distribution: "consumer-distribution-1",
            url: "http://consumer-distribution-1.com",
            uploadStateInterval: "{ \"length\": 20, \"unit\": \"SECONDS\" }"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"removeDistributionProvider":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          removeDistributionProvider (
            distribution: "consumer-distribution"
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"distributionProvidersInfo":[{"distributionName":"consumer-distribution-1","distributionUrl":"http://consumer-distribution-1.com"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          distributionProvidersInfo {
             distributionName,
             distributionUrl
          }
        }
      """)))
  }
}
