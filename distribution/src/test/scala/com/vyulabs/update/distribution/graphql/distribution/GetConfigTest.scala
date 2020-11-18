package com.vyulabs.update.distribution.graphql.distribution

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.DistributionClientInfoDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetConfigTest extends TestEnvironment {
  behavior of "Config Client Request"

  override def beforeAll() = {
    val clientsInfoCollection = result(collections.Developer_ClientsInfo)

    result(clientsInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("client1", DistributionClientConfig("common", Some("test"))))))
  }

  it should "get config for client" in {
    val graphqlContext = new GraphqlContext("distribution", VersionHistoryConfig(5), distributionDir, collections, UserInfo("client1", UserRole.Distribution))

    assertResult((OK,
      ("""{"data":{"config":{"installProfile":"common","testClientMatch":"test"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        query {
          config {
            installProfile,
            testClientMatch
          }
        }
      """)))
  }
}
