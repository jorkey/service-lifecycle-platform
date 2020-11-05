package com.vyulabs.update.distribution.graphql.client

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.{DistributionDirectory, GraphqlTestEnvironment}
import com.vyulabs.update.info.{DesiredVersion, DeveloperDesiredVersions, InstalledDesiredVersions, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class TestedVersionsTest extends GraphqlTestEnvironment {
  behavior of "Tested Versions Info Requests"

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_ClientsProfiles)
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(installProfileCollection.insert(ClientProfile("common", Set("service1", "service2"))))

    result(clientInfoCollection.insert(ClientInfo("test-client", ClientConfig("common", None))))
    result(clientInfoCollection.insert(ClientInfo("client1", ClientConfig("common", Some("test-client")))))
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("test-client", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation {
          setTestedVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.1" },
               { serviceName: "service2", buildVersion: "2.1.1" }
            ]
          )
        }
      """)))

    val graphqlContext2 = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"1.1.1"},{"serviceName":"service2","buildVersion":"2.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext2, graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))

    result(collections.State_TestedVersions.map(_.dropItems()))
  }

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Desired versions for profile common are not tested by anyone","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Administrator))
    result(collections.State_TestedVersions.map(_.insert(
      TestedDesiredVersions("common", Seq(DesiredVersion("service1", BuildVersion(1, 1, 0))), Seq(TestSignature("test-client", new Date()))))))
    result(collections.Developer_DesiredVersions.map(_.insert(
      DeveloperDesiredVersions(Some("client1"), Seq(DesiredVersion("service1", BuildVersion("client1", 1, 1)))))))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Client required preliminary testing shouldn't have personal desired versions","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))
    result(collections.Client_DesiredVersions.map(_.dropItems()))
  }
}
