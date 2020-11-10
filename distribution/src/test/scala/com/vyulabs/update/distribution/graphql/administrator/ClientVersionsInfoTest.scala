package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.GraphqlTestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class ClientVersionsInfoTest extends GraphqlTestEnvironment {
  behavior of "Client Versions Requests"

  val graphqlContext = GraphqlContext(VersionHistoryConfig(3), distributionDir, collections, UserInfo("admin", UserRole.Administrator))

  it should "add/get client version info" in {
    addClientVersionInfo("service1", BuildVersion(1, 1, 1))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"1.1.1","buildInfo":{"author":"author1"},"installInfo":{"user":"admin"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientVersionsInfo (service: "service1", version: "1.1.1") {
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

    removeClientVersion("service1", BuildVersion(1, 1, 1))
  }

  it should "remove obsolete client versions" in {
    addClientVersionInfo("service1", BuildVersion(1))
    addClientVersionInfo("service1", BuildVersion(2))
    addClientVersionInfo("service1", BuildVersion(3))
    addClientVersionInfo("service1", BuildVersion(4))
    addClientVersionInfo("service1", BuildVersion(5))
    addClientVersionInfo("service2", BuildVersion(1))
    addClientVersionInfo("service3", BuildVersion(2))

    assertResult((OK,
      ("""{"data":{"clientVersionsInfo":[{"version":"3"},{"version":"4"},{"version":"5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientVersionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeClientVersion("service1", BuildVersion(3))
    removeClientVersion("service1", BuildVersion(4))
    removeClientVersion("service1", BuildVersion(5))
  }

  def addClientVersionInfo(serviceName: ServiceName, version: BuildVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addClientVersionInfo":{"version":"${version.toString}"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation AddClientVersionInfo($$service: String!, $$version: BuildVersion!, $$buildDate: Date!, $$installDate: Date!) {
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
                      }) {
                      version
                    }
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson, "buildDate" -> new Date().toJson, "installDate" -> new Date().toJson))))
  }

  def removeClientVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeClientVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation RemoveClientVersion($$service: String!, $$version: BuildVersion!) {
                    removeClientVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
  }
}
