package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlContext
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import com.vyulabs.update.common.info.{AccessToken, ClientDesiredVersion, DirectoryServiceState, DistributionConsumerInfo, DistributionServiceState, ServiceState, UserInfo, UserRole}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetStateInfoTest extends TestEnvironment {
  behavior of "State Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    result(collections.Distribution_ConsumersInfo.insert(
      DistributionConsumerInfo("distribution1", "common", Some("test"))))

    result(collections.State_InstalledDesiredVersions.insert(
      InstalledDesiredVersions("distribution1", Seq(
        ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
        ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 3)))))))))

    result(collections.State_ServiceStates.insert(
      DistributionServiceState(distributionName, "instance1", DirectoryServiceState("distribution", "directory1",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 2, 3))))), None, None, None, None)))))

    result(collections.State_ServiceStates.insert(
      DistributionServiceState("distribution1", "instance2", DirectoryServiceState("service1", "directory2",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("distribution1", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None)))))
  }

  it should "return own service version" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", UserRole.Administrator)), workspace)
    assertResult((OK,
      ("""{"data":{"serviceStates":[{"instance":{"service":{"version":"test-1.2.3"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query ServicesStateQuery($$directory: String!) {
          serviceStates (distribution: "test", service: "distribution", directory: $$directory) {
            instance  {
              service {
                version
              }
            }
          }
        }
      """, None, variables = JsObject("directory" -> JsString("directory1")))))
  }

  it should "return service state" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", UserRole.Administrator)), workspace)
    assertResult((OK,
      ("""{"data":{"serviceStates":[{"instance":{"instanceId":"instance2","service":{"version":"distribution1-1.1.0"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          serviceStates (distribution: "distribution1", service: "service1") {
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

  it should "return installed versions" in {
    val graphqlContext = GraphqlContext(Some(AccessToken("admin", UserRole.Administrator)), workspace)
    assertResult((OK,
      ("""{"data":{"installedDesiredVersions":[{"serviceName":"service1","version":"test-1.1.1"},{"serviceName":"service2","version":"test-2.1.3"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          installedDesiredVersions (distribution: "distribution1") {
             serviceName
             version
          }
        }
      """)))
  }
}
