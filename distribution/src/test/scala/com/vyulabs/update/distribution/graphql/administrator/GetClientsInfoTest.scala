package com.vyulabs.update.distribution.graphql.administrator

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, TestEnvironment}
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.{ClientInfoDocument, DatabaseCollections, MongoDb}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetClientsInfoTest extends TestEnvironment {
  behavior of "Client Info Requests"

  val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("admin", UserRole.Administrator))

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(clientInfoCollection.insert(ClientInfoDocument(ClientInfo("client1", ClientConfig("common", Some("test"))))))
  }

  it should "get user info" in {
    assertResult((OK,
      ("""{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          userInfo {
            name
            role
          }
        }
      """))
    )
  }

  it should "get clients info" in {
    assertResult((OK,
      ("""{"data":{"clientsInfo":[{"clientName":"client1","clientConfig":{"installProfile":"common","testClientMatch":"test"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
        query {
          clientsInfo {
            clientName
            clientConfig {
              installProfile
              testClientMatch
            }
          }
        }
      """))
    )
  }
}
