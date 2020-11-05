package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, GraphqlTestEnvironment}
import com.vyulabs.update.info.{ClientServiceState, DesiredVersion, InstalledDesiredVersions, ServiceState}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class StateInfoTest extends GraphqlTestEnvironment {
  behavior of "State Info Requests"

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_ClientsInfo)
    val installedVersionsCollection = result(collections.State_InstalledDesiredVersions)
    val clientServiceStatesCollection = result(collections.State_ServiceStates)

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", Some("test")))))

    result(installedVersionsCollection.insert(
      InstalledDesiredVersions("client1", Seq(DesiredVersion("service1", BuildVersion(1, 1, 1)), DesiredVersion("service2", BuildVersion(2, 1, 3))))))

    result(clientServiceStatesCollection.insert(
      ClientServiceState("client1", "instance1", "service1", "directory1",
        ServiceState(date = new Date(), None, None, version = Some(BuildVersion(1, 1, 0)), None, None, None, None))))
  }

  it should "return own service version" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"state":{"version":"1.2.3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query DistributionVersionQuery($$directory: String!) {
          servicesState (client: "own", service: "distribution", directory: $$directory) {
            state  {
              version
            }
          }
        }
      """
        ,
        None, variables = JsObject("directory" -> JsString(ownServicesDir.getCanonicalPath)))))
  }

  it should "return installed versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"installedDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.1"},{"serviceName":"service2","buildVersion":"2.1.3"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          installedDesiredVersions (client: "client1") {
             serviceName
             buildVersion
          }
        }
      """)))
  }

  it should "return service state" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instanceId":"instance1","state":{"version":"1.1.0"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instanceId
            state {
              version
            }
          }
        }
      """))
    )
  }
}
