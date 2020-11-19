package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{DistributionClientInfoDocument, DistributionClientProfileDocument}
import scala.concurrent.ExecutionContext
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class ClientDesiredVersionsTest extends TestEnvironment {
  behavior of "Client Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_ClientsProfiles)
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(installProfileCollection.insert(DistributionClientProfileDocument(DistributionClientProfile("common", Set("service1", "service2")))))
    result(clientInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("client2", DistributionClientConfig("common", None)))))
  }

  it should "set/get client desired versions" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), collections, distributionDir, UserInfo("admin", UserRole.Administrator))

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
  }
}
