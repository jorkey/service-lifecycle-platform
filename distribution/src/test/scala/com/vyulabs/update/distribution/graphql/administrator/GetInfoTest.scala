package com.vyulabs.update.distribution.graphql.administrator

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, TestEnvironment}
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.{DistributionClientInfoDocument, DatabaseCollections, MongoDb}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetInfoTest extends TestEnvironment {
  behavior of "Misc Info Requests"

  val graphqlContext = new GraphqlContext("distribution", versionHistoryConfig, distributionDir, collections, UserInfo("admin", UserRole.Administrator))

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(clientInfoCollection.insert(DistributionClientInfoDocument(DistributionClientInfo("client1", DistributionClientConfig("common", Some("test"))))))
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
      ("""{"data":{"distributionClientsInfo":[{"clientName":"client1","clientConfig":{"installProfile":"common","testClientMatch":"test"}}]}}""").parseJson))(
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
