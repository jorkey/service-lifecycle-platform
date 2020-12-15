package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo, DistributionClientProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.{DistributionClientInfoDocument, DistributionClientProfileDocument}
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import com.vyulabs.update.distribution.mongo.DistributionClientProfileDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class DeveloperDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_DistributionClientsProfiles)
    val clientInfoCollection = result(collections.Developer_DistributionClientsInfo)

    result(installProfileCollection.insert(DistributionClientProfileDocument(DistributionClientProfile("common", Set("service1", "service2")))))
    result(clientInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("client2", DistributionClientConfig("common", None)))))
  }

  it should "set/get developer desired versions" in {
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

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
  }
}
