package com.vyulabs.update.distribution.graphql.administrator

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.GraphqlTestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, ClientProfileDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class ClientDesiredVersionsTest extends GraphqlTestEnvironment {
  behavior of "Client Desired Versions Requests"

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_ClientsProfiles)
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(installProfileCollection.insert(ClientProfileDocument(ClientProfile("common", Set("service1", "service2")))))
    result(clientInfoCollection.insert(ClientInfoDocument(ClientInfo("client2", ClientConfig("common", None)))))
  }

  it should "set/get client desired versions" in {
    val graphqlContext = new GraphqlContext(VersionHistoryConfig(5), distributionDir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"setClientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setClientDesiredVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.2"},
               { serviceName: "service2", buildVersion: "2.1.4"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (services: ["service1"]) {
             serviceName
             buildVersion
          }
        }
      """)))
  }
}
