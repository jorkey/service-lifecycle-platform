package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.{TestEnvironment}
import com.vyulabs.update.info.{ClientDesiredVersion, DirectoryServiceState, DistributionServiceState, ServiceState}
import com.vyulabs.update.info.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.{DistributionClientInfoDocument, InstalledDesiredVersionsDocument, ServiceStateDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetStateInfoTest extends TestEnvironment {
  behavior of "State Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_DistributionClientsInfo)
    val installedVersionsCollection = result(collections.State_InstalledDesiredVersions)
    val serviceStatesCollection = result(collections.State_ServiceStates)

    result(clientInfoCollection.insert(DistributionClientInfoDocument(
      DistributionClientInfo("distribution1", DistributionClientConfig("common", Some("test"))))))

    result(installedVersionsCollection.insert(
      InstalledDesiredVersionsDocument("distribution1", Seq(
        ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 1, 1))))),
        ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2, 1, 3)))))))))

    result(serviceStatesCollection.insert(ServiceStateDocument(0,
      DistributionServiceState(distributionName, "instance1", DirectoryServiceState("distribution", "directory1",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1, 2, 3))))), None, None, None, None))))))

    result(serviceStatesCollection.insert(ServiceStateDocument(1,
      DistributionServiceState("distribution1", "instance2", DirectoryServiceState("service1", "directory2",
        ServiceState(date = new Date(), None, None, version =
          Some(ClientDistributionVersion("distribution1", ClientVersion(DeveloperVersion(Seq(1, 1, 0))))), None, None, None, None))))))
  }

  it should "return own service version" in {
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instance":{"service":{"version":"test-1.2.3"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query ServicesStateQuery($$directory: String!) {
          servicesState (distribution: "test", service: "distribution", directory: $$directory) {
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
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instance":{"instanceId":"instance2","service":{"version":"distribution1-1.1.0"}}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          servicesState (distribution: "distribution1", service: "service1") {
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
    val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)
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
