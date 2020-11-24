package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import com.vyulabs.update.version.ClientDistributionVersion
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class ClientVersionsInfoTest extends TestEnvironment {
  behavior of "Client Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  it should "add/get client version info" in {
    addClientVersionInfo("service1", ClientDistributionVersion.parse("test-1.1.1"))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"test-1.1.1","buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", version: "test-1.1.1") {
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

    removeClientVersion("service1", ClientDistributionVersion.parse("test-1.1.1"))
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
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
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
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
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
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson, "buildDate" -> new Date().toJson, "installDate" -> new Date().toJson))))
    assert(distributionDir.getClientVersionImageFile(serviceName, version).createNewFile())
  }

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeClientVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation RemoveClientVersion($$service: String!, $$version: ClientDistributionVersion!) {
                    removeClientVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
    assert(!distributionDir.getClientVersionImageFile(serviceName, version).exists())
  }
}
