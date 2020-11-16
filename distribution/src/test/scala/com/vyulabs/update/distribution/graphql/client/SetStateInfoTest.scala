package com.vyulabs.update.distribution.graphql.client

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientDesiredVersion, DeveloperDesiredVersion, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, InstalledDesiredVersionsDocument, TestedDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.utils.Utils.DateJson._

class SetStateInfoTest extends TestEnvironment {
  behavior of "Tested Versions Info Requests"

  override protected def beforeAll(): Unit = {
    result(collections.Developer_ClientsInfo.map(_.insert(ClientInfoDocument(ClientInfo("client1", ClientConfig("common", Some("test")))))))
  }

  it should "set tested versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"setTestedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setTestedVersions (
            versions: [
              { serviceName: "service1", version: "test-1.1.2" },
              { serviceName: "service2", version: "test-2.1.2" }
            ]
          )
        }
      """)))

    val date = new Date()
    result(collections.State_TestedVersions.map(v => result(v.find().map(_.map(v => TestedDesiredVersionsDocument(TestedDesiredVersions(
      v.versions.profileName, v.versions.versions, v.versions.signatures.map(s => TestSignature(s.clientName, date))))))
      .map(assertResult(_)(Seq(TestedDesiredVersionsDocument(TestedDesiredVersions("common", Seq(
        DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 2)))),
        DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2, 1, 2))))),
        Seq(TestSignature("client1", date))))))))))
    result(collections.State_TestedVersions.map(_.dropItems()))
  }

  it should "set installed desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"setInstalledDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          setInstalledDesiredVersions (
            versions: [
               { serviceName: "service1", version: "test-1.1.1" },
               { serviceName: "service2", version: "test-2.1.1" }
            ]
          )
        }
      """)))

    result(collections.State_InstalledDesiredVersions.map(v => result(v.find().map(assertResult(Seq(InstalledDesiredVersionsDocument("client1", Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 1)))))))))(_)))))
    result(collections.State_InstalledDesiredVersions.map(_.dropItems()))
  }

  it should "set services state" in {
    val graphqlContext1 = GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"setServicesState":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation ServicesState($$date: Date!) {
          setServicesState (
            state: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  service: { date: $$date, version: "test-1.2.3" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"setServicesState":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation ServicesState($$date: Date!) {
          setServicesState (
            state: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  service: { date: $$date, version: "test-1.2.4" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    val graphqlContext2 = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("client1", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instance":{"instanceId":"instance1","service":{"version":"test-1.2.4"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext2, graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instance {
              instanceId
              service {
                version
              }
            }
          }
        }
      """))
    )

    result(collections.State_ServiceStates.map(_.dropItems()))
  }
}
