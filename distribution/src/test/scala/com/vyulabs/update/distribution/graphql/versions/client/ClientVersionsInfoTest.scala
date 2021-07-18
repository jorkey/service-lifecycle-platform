package com.vyulabs.update.distribution.graphql.versions.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.ServiceId
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class ClientVersionsInfoTest extends TestEnvironment {
  behavior of "Client Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  it should "add/get/remove client version info" in {
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-1.1.1_1"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("distribution1-2.1.3_1"))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":{"distribution":"test","developerBuild":[1,1,1],"clientBuild":1},"buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", distribution: "test", version: { developerBuild: [1, 1, 1], clientBuild: 1 } ) {
            version { distribution, developerBuild, clientBuild }
            buildInfo { author }
            installInfo { user }
          }
        }
      """
    )))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":{"distribution":"distribution1","developerBuild":[2,1,3],"clientBuild":1},"buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", distribution: "distribution1") {
            version { distribution, developerBuild, clientBuild }
            buildInfo { author }
            installInfo { user }
          }
        }
      """
      )))

    removeClientVersion("service1", ClientDistributionVersion.parse("test-1.1.1_1"))
    removeClientVersion("service1", ClientDistributionVersion.parse("distribution1-2.1.3_1"))
  }

  it should "remove obsolete client versions" in {
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-1"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-2"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-3"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-4"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-5"))
    addClientVersionInfo("service2", ClientDistributionVersion.parse("test-1"))
    addClientVersionInfo("service3", ClientDistributionVersion.parse("test-2"))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":{"distribution":"test","developerBuild":[3],"clientBuild":0}},{"version":{"distribution":"test","developerBuild":[4],"clientBuild":0}},{"version":{"distribution":"test","developerBuild":[5],"clientBuild":0}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1") {
            version { distribution, developerBuild, clientBuild }
          }
        }
      """)))

    removeClientVersion("service1", ClientDistributionVersion.parse("test-3"))
    removeClientVersion("service1", ClientDistributionVersion.parse("test-4"))
    removeClientVersion("service1", ClientDistributionVersion.parse("test-5"))
  }

  def addClientVersionInfo(service: ServiceId, version: ClientDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addClientVersionInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, builderContext,
        graphql"""
                  mutation AddClientVersionInfo($$service: String!, $$version: ClientDistributionVersionInput!, $$buildDate: Date!, $$installDate: Date!) {
                    addClientVersionInfo (
                      info: {
                        service: $$service,
                        version: $$version,
                        buildInfo: {
                          author: "author1",
                          branches: [ "master" ],
                          time: $$buildTime
                        },
                        installInfo: {
                          user: "admin",
                          time: $$installTime
                        }
                      })
                  }
                """,
        variables = JsObject(
          "service" -> JsString(service),
          "version" -> version.toJson,
          "buildTime" -> new Date().toJson,
          "installTime" -> new Date().toJson))))
    assert(distributionDir.getClientVersionImageFile(service, version).createNewFile())
  }

  def removeClientVersion(service: ServiceId, version: ClientDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeClientVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext,
        graphql"""
                  mutation RemoveClientVersion($$service: String!, $$version: ClientDistributionVersionInput!) {
                    removeClientVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(service), "version" -> version.toJson))))
    assert(!distributionDir.getClientVersionImageFile(service, version).exists())
  }
}
