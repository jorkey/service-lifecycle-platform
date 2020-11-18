package com.vyulabs.update.distribution.graphql.service

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientDesiredVersion, DeveloperDesiredVersion}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientDesiredVersionsDocument, DeveloperDesiredVersionsDocument}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetDesiredVersionsTest extends TestEnvironment {
  behavior of "Desired Versions Service Requests"

  override def beforeAll() = {
    val desiredVersionsCollection = result(collections.Client_DesiredVersions)

    desiredVersionsCollection.insert(ClientDesiredVersionsDocument(Seq(
      ClientDesiredVersion("service1", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))),
      ClientDesiredVersion("service2", ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))))))
  }

  it should "get desired versions for service" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), distributionDir, collections, UserInfo("service1", UserRole.Service))

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
}
