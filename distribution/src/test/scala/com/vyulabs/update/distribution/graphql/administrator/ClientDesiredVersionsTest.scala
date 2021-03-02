package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.common.info.{UserInfo, UserRole}

import scala.concurrent.ExecutionContext
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class ClientDesiredVersionsTest extends TestEnvironment {
  behavior of "Client Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Developer_DistributionClientsProfiles.insert(DistributionClientProfile("common", Set("service1", "service2"))))
    result(collections.Developer_DistributionClientsInfo.insert(DistributionClientInfo("client2", DistributionClientConfig("common", None))))
  }

  it should "set/get client desired versions" in {
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { serviceName: "service1", version: "test-1.1.2" },
               { serviceName: "service2", version: "test-2.1.4" }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","version":"test-1.1.2"},{"serviceName":"service2","version":"test-2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             version
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","version":"test-1.1.2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (services: ["service1"]) {
             serviceName
             version
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { serviceName: "service1" },
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service2","version":"test-2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             version
          }
        }
      """)))
  }
}
