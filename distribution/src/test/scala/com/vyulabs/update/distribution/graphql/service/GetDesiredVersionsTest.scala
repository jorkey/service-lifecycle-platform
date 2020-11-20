package com.vyulabs.update.distribution.graphql.service

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientDesiredVersion, DeveloperDesiredVersion}
import distribution.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientDesiredVersionsDocument, DeveloperDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Desired Versions Service Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override val dbName = super.dbName + "-service"

  override def beforeAll() = {
    val desiredVersionsCollection = result(collections.Client_DesiredVersions)

    desiredVersionsCollection.insert(ClientDesiredVersionsDocument(Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))))))
  }

  it should "get desired versions for service" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), collections, distributionDir, UserInfo("service1", UserRole.Service))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","version":"test-1"},{"serviceName":"service2","version":"test-2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
             serviceName
             version
          }
        }
      """)))
  }

  it should "get desired version for specified service" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), collections, distributionDir, UserInfo("service1", UserRole.Service))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"version":"test-1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions (services: ["service1"]) {
             version
          }
        }
      """)))
  }
}
