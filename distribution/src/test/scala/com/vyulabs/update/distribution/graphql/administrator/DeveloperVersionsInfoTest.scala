package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date
import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.{GraphqlTestEnvironment}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class DeveloperVersionsInfoTest extends GraphqlTestEnvironment {
  behavior of "Developer Versions Requests"

  val graphqlContext = GraphqlContext(VersionHistoryConfig(3), distributionDir, collections, UserInfo("admin", UserRole.Administrator))

  it should "add/get/remove developer version info" in {
    addDeveloperVersionInfo("service1", BuildVersion(1, 1, 1))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"1.1.1","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerVersionsInfo (service: "service1", version: "1.1.1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
    )))

    removeDeveloperVersion("service1", BuildVersion(1, 1, 1))
  }

  it should "get developer versions info" in {
    addDeveloperVersionInfo("service1", BuildVersion(1, 1, 1))
    addDeveloperVersionInfo("service1", BuildVersion(1, 1, 2))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"1.1.1","buildInfo":{"author":"author1"}},{"version":"1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerVersionsInfo (service: "service1") {
            version
            buildInfo {
              author
            }
          }
        }
      """))
    )

    removeDeveloperVersion("service1", BuildVersion(1, 1, 1))
    removeDeveloperVersion("service1", BuildVersion(1, 1, 2))
  }

  it should "add/get developer for client version info" in {
    addDeveloperVersionInfo("service1", BuildVersion("client1", 1, 1, 2))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"client1-1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerVersionsInfo (service: "service1", client: "client1") {
            version
            buildInfo {
              author
            }
          }
        }
      """)))

    removeDeveloperVersion("service1", BuildVersion("client1", 1, 1, 2))
  }

  it should "remove obsolete developer versions" in {
    addDeveloperVersionInfo("service1", BuildVersion(1))
    addDeveloperVersionInfo("service1", BuildVersion(2))
    addDeveloperVersionInfo("service1", BuildVersion(3))
    addDeveloperVersionInfo("service1", BuildVersion(4))
    addDeveloperVersionInfo("service1", BuildVersion(5))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"3"},{"version":"4"},{"version":"5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          developerVersionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeDeveloperVersion("service1", BuildVersion(3))
    removeDeveloperVersion("service1", BuildVersion(4))
    removeDeveloperVersion("service1", BuildVersion(5))
  }

  def addDeveloperVersionInfo(serviceName: ServiceName, version: BuildVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addDeveloperVersionInfo":{"version":"${version.toString}"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation AddDeveloperVersionInfo($$service: String!, $$version: BuildVersion!, $$date: Date!) {
                    addDeveloperVersionInfo (
                      service: $$service,
                      version: $$version,
                      buildInfo: {
                        author: "author1",
                        branches: [ "master" ]
                        date: $$date
                      }) {
                      version
                    }
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson, "date" -> new Date().toJson))))
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeDeveloperVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation RemoveDeveloperVersion($$service: String!, $$version: BuildVersion!) {
                    removeDeveloperVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
  }
}
