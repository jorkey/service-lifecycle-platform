package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, TestEnvironment}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientServiceState, DeveloperDesiredVersion, DirectoryServiceState, ServiceState}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, InstalledDesiredVersionsDocument, ServiceStateDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetStateInfoTest extends TestEnvironment {
  behavior of "State Info Requests"

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_ClientsInfo)
    val installedVersionsCollection = result(collections.State_InstalledDesiredVersions)
    val serviceStatesCollection = result(collections.State_ServiceStates)

    result(clientInfoCollection.insert(ClientInfoDocument(
      ClientInfo("client1", ClientConfig("common", Some("test"))))))

    result(installedVersionsCollection.insert(
      InstalledDesiredVersionsDocument("client1", Seq(
        ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
        ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 3)))))))))

    result(serviceStatesCollection.insert(ServiceStateDocument(0,
      ClientServiceState("client1", "instance1", DirectoryServiceState("service1", "directory1",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None))))))
  }

  it should "return own service version" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instance":{"service":{"version":"test-1.2.3"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query ServicesStateQuery($$directory: String!) {
          servicesState (client: "own", service: "distribution", directory: $$directory) {
            instance  {
              service {
                version
              }
            }
          }
        }
      """, None, variables = JsObject("directory" -> JsString(ownServicesDir.getCanonicalPath)))))
  }

  it should "return installed versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"installedDesiredVersions":[{"serviceName":"service1","version":"test-1.1.1"},{"serviceName":"service2","version":"test-2.1.3"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          installedDesiredVersions (client: "client1") {
             serviceName
             version
          }
        }
      """)))
  }

  it should "return service state" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instance":{"instanceId":"instance1","service":{"version":"test-1.1.0"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instance  {
              instanceId
              service {
                version
              }
            }
          }
        }
      """))
    )
  }
}
