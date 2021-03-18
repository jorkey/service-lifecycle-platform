package com.vyulabs.update.distribution.graphql.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.info.{AccessToken, UserRole}
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class ClientVersionsInfoTest extends TestEnvironment {
  behavior of "Client Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val adminContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)
  val builderContext = GraphqlContext(Some(AccessToken("builder", Seq(UserRole.Builder))), workspace)

  it should "add/get/remove client version info" in {
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-1.1.1_1"))
    addClientVersionInfo("service1", ClientDistributionVersion.parse("distribution1-2.1.3_1"))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"test-1.1.1_1","buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", distribution: "test", version: "1.1.1_1") {
            version
            buildInfo {
              author
            }
            installInfo {
              user
            }
          }
        }
      """
    )))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"distribution1-2.1.3_1","buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", distribution: "distribution1") {
            version
            buildInfo {
              author
            }
            installInfo {
              user
            }
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
      ("""{"data":{"clientVersionsInfo":[{"version":"test-3"},{"version":"test-4"},{"version":"test-5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          clientVersionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeClientVersion("service1", ClientDistributionVersion.parse("test-3"))
    removeClientVersion("service1", ClientDistributionVersion.parse("test-4"))
    removeClientVersion("service1", ClientDistributionVersion.parse("test-5"))
  }

  def addClientVersionInfo(serviceName: ServiceName, version: ClientDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addClientVersionInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.BuilderSchemaDefinition, builderContext,
        graphql"""
                  mutation AddClientVersionInfo($$service: String!, $$version: ClientDistributionVersion!, $$buildDate: Date!, $$installDate: Date!) {
                    addClientVersionInfo (
                      info: {
                        serviceName: $$service,
                        version: $$version,
                        buildInfo: {
                          author: "author1",
                          branches: [ "master" ],
                          date: $$buildDate
                        },
                        installInfo: {
                          user: "admin",
                          date: $$installDate
                        }
                      })
                  }
                """,
        variables = JsObject(
          "service" -> JsString(serviceName),
          "version" -> version.toJson,
          "buildDate" -> new Date().toJson,
          "installDate" -> new Date().toJson))))
    assert(distributionDir.getClientVersionImageFile(serviceName, version).createNewFile())
  }

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeClientVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext,
        graphql"""
                  mutation RemoveClientVersion($$service: String!, $$version: ClientDistributionVersion!) {
                    removeClientVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
    assert(!distributionDir.getClientVersionImageFile(serviceName, version).exists())
  }
}
