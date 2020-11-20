package com.vyulabs.update.distribution.graphql.distribution

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.DeveloperDesiredVersion
import distribution.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{DeveloperDesiredVersionsDocument, DistributionClientInfoDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Client Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override val dbName = super.dbName + "-distribution"

  override def beforeAll() = {
    val clientsInfoCollection = result(collections.Developer_DistributionClientsInfo)
    val desiredVersionsCollection = result(collections.Developer_DesiredVersions)

    result(clientsInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("distribution1", DistributionClientConfig("common", None)))))

    desiredVersionsCollection.insert(DeveloperDesiredVersionsDocument(Seq(
      DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1)))),
      DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2)))))))
  }

  it should "get desired versions for client distribution" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), collections, distributionDir, UserInfo("distribution1", UserRole.Distribution))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","version":"test-1"},{"serviceName":"service2","version":"test-2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
             serviceName
             version
          }
        }
      """)))
  }
}
