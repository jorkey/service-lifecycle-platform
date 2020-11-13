package com.vyulabs.update.distribution.graphql.administrator

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, ClientProfileDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class DeveloperDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Requests"

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_ClientsProfiles)
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(installProfileCollection.insert(ClientProfileDocument(ClientProfile("common", Set("service1", "service2")))))
    result(clientInfoCollection.insert(ClientInfoDocument(ClientInfo("client2", ClientConfig("common", None)))))
  }

  it should "set/get common developer desired versions and personal desired versions for client" in {
    val graphqlContext = new GraphqlContext(VersionHistoryConfig(5), distributionDir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.2"},
               { serviceName: "service2", buildVersion: "2.1.4"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            client: "client1",
            versions: [
               { serviceName: "service1", buildVersion: "client1-2.1.4"},
               { serviceName: "service3", buildVersion: "client1-3.1.2"}
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))
  }

  it should "set/get developer personal client desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            client: "client2",
            versions: [
               { serviceName: "service2", buildVersion: "client2-1.1.1" }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions (client: "client2") {
             serviceName
             buildVersion
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            client: "client2",
            versions: []
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions (client: "client2") {
             serviceName
             buildVersion
          }
        }
      """)))
  }

  it should "return client merged desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.2"},
               { serviceName: "service2", buildVersion: "2.1.4"}
            ]
          )
        }
      """)))


    assertResult((OK,
      ("""{"data":{"setDeveloperDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setDeveloperDesiredVersions (
            client: "client2",
            versions: [
               { serviceName: "service2", buildVersion: "client2-1.1.1" }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"developerDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerDesiredVersions (client: "client2", merged: true) {
             serviceName
             buildVersion
          }
        }
      """))
    )

    result(collections.Developer_DesiredVersions.map(_.dropItems()))
  }
}
