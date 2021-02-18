package com.vyulabs.update.distribution.graphql.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info.{ClientDesiredVersion, ClientDesiredVersions, UserInfo, UserRole}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Desired Versions Service Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def dbName = super.dbName + "-service"

  override def beforeAll() = {
    result(collections.Client_DesiredVersions.insert(ClientDesiredVersions(Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2)))))))))
  }

  it should "get desired versions for service" in {
    val graphqlContext = new GraphqlContext(UserInfo("service", UserRole.Service), workspace)

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","version":"test-1"},{"serviceName":"service2","version":"test-2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions {
             serviceName
             version
          }
        }
      """)))
  }

  it should "get desired version for specified service" in {
    val graphqlContext = new GraphqlContext(UserInfo("service", UserRole.Service), workspace)

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"version":"test-1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (services: ["service1"]) {
             version
          }
        }
      """)))
  }
}
