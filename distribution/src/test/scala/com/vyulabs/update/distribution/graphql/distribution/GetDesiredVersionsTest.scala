package com.vyulabs.update.distribution.graphql.distribution

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.DeveloperDesiredVersion
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{DistributionClientInfoDocument, DeveloperDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Developer Desired Versions Client Requests"

  override def beforeAll() = {
    val clientsInfoCollection = result(collections.Developer_ClientsInfo)
    val desiredVersionsCollection = result(collections.Developer_DesiredVersions)

    result(clientsInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("client1", DistributionClientConfig("common", None)))))

    desiredVersionsCollection.insert(DeveloperDesiredVersionsDocument(Seq(
      DeveloperDesiredVersion("service1", DeveloperDistributionVersion("test", DeveloperVersion(Seq(1)))),
      DeveloperDesiredVersion("service2", DeveloperDistributionVersion("test", DeveloperVersion(Seq(2)))))))
  }

  it should "get desired versions for client" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), distributionDir, collections, UserInfo("client1", UserRole.Distribution))

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
