package com.vyulabs.update.distribution.graphql.client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.info.{AccessToken, UserRole}
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class DeveloperVersionsInfoTest extends TestEnvironment {
  behavior of "Developer Versions Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val adminContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)
  val builderContext = GraphqlContext(Some(AccessToken("builder", Seq(UserRole.Builder))), workspace)

  it should "add/get/remove developer version info" in {
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("distribution1", DeveloperVersion(Seq(2, 1, 3))))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"test-1.1.1","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          developerVersionsInfo (service: "service1", distribution: "test", version: "1.1.1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
    )))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"distribution1-2.1.3","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          developerVersionsInfo (service: "service1", distribution: "distribution1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
      )))

    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))
    removeDeveloperVersion("service1", DeveloperDistributionVersion("distribution1", DeveloperVersion(Seq(2, 1, 3))))
  }

  it should "get developer versions info" in {
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 2))))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"test-1.1.1","buildInfo":{"author":"author1"}},{"version":"test-1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
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

    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 1))))
    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1, 1, 2))))
  }

  it should "remove obsolete developer versions" in {
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(3))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(4))))
    addDeveloperVersionInfo("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(5))))

    assertResult((OK,
      ("""{"data":{"developerVersionsInfo":[{"version":"test-3"},{"version":"test-4"},{"version":"test-5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext, graphql"""
        query {
          developerVersionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(3))))
    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(4))))
    removeDeveloperVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(5))))
  }

  def addDeveloperVersionInfo(serviceName: ServiceName, version: DeveloperDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addDeveloperVersionInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.BuilderSchemaDefinition, builderContext,
        graphql"""
                  mutation AddDeveloperVersionInfo($$service: String!, $$version: DeveloperDistributionVersion!, $$date: Date!) {
                    addDeveloperVersionInfo (
                      info: {
                        serviceName: $$service,
                        version: $$version,
                        buildInfo: {
                          author: "author1",
                          branches: [ "master" ]
                          date: $$date
                        }
                      })
                  }
                """,
        variables = JsObject(
          "service" -> JsString(serviceName),
          "version" -> version.toJson,
          "date" -> new Date().toJson))))
    assert(distributionDir.getDeveloperVersionImageFile(serviceName, version).createNewFile())
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"removeDeveloperVersion":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, adminContext,
        graphql"""
                  mutation RemoveDeveloperVersion($$service: String!, $$version: DeveloperDistributionVersion!) {
                    removeDeveloperVersion (service: $$service, version: $$version)
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson))))
    assert(!distributionDir.getDeveloperVersionImageFile(serviceName, version).exists())
  }
}