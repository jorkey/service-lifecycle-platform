package com.vyulabs.update.distribution.graphql.client

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.ClientInfoDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetConfigTest extends TestEnvironment {
  behavior of "Config Client Request"

  override def beforeAll() = {
    val clientsInfoCollection = result(collections.Developer_ClientsInfo)

    result(clientsInfoCollection.insert(ClientInfoDocument(ClientInfo("client1", ClientConfig("common", Some("test"))))))
  }

  it should "get config for client" in {
    val graphqlContext = new GraphqlContext(VersionHistoryConfig(5), distributionDir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"config":{"installProfile":"common","testClientMatch":"test"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          config {
            installProfile,
            testClientMatch
          }
        }
      """)))
  }
}
