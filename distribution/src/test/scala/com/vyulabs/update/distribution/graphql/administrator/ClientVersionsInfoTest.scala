package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class ClientVersionsInfoTest extends TestEnvironment {
  behavior of "Client Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = GraphqlContext("distribution", VersionHistoryConfig(3), collections, distributionDir, UserInfo("admin", UserRole.Administrator))

  it should "add/get client version info" in {
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))

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

    removeClientVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))
  }

  it should "remove obsolete client versions" in {
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1))))
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2))))
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(3))))
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(4))))
    addClientVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(5))))
    addClientVersionInfo("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1))))
    addClientVersionInfo("service3", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2))))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"3"},{"version":"4"},{"version":"5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientVersionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeClientVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(3))))
    removeClientVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(4))))
    removeClientVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(5))))
  }

  def addClientVersionInfo(serviceName: ServiceName, version: DeveloperDistributionVersion): Unit = {
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
  }

  def removeClientVersion(serviceName: ServiceName, version: DeveloperDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeClientVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation RemoveClientVersion($$service: String!, $$version: ClientDistributionVersion!) {
                    removeClientVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
  }
}
