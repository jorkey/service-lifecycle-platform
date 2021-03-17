package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{AccessToken, DistributionConsumerInfo, DistributionConsumerProfile, UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DeveloperDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumerProfiles.insert(DistributionConsumerProfile("common", Seq("service1", "service2"))))
    result(collections.Distribution_ConsumersInfo.insert(DistributionConsumerInfo("client2", "common", None)))
  }

  it should "set/get developer desired versions" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", UserRole.Administrator)), workspace)

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service1", version: "test-1.1.2"},
               { serviceName: "service2", version: "test-2.1.4"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","version":"test-1.1.2"},{"serviceName":"service2","version":"test-2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             version
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service2"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","version":"test-1.1.2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             version
          }
        }
      """)))
  }
}
