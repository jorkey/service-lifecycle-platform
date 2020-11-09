package com.vyulabs.update.distribution.graphql.administrator

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.{DistributionDirectory, GraphqlTestEnvironment}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.IoUtils
import distribution.config.VersionHistoryConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.{DatabaseCollections, MongoDb}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class ClientsInfoTest extends GraphqlTestEnvironment {
  behavior of "Client Info Requests"

  val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("admin", UserRole.Administrator))

  override def beforeAll() = {
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(clientInfoCollection.insert(ClientInfo("client1", ClientConfig("common", Some("test")))))
  }

  it should "return user info" in {
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

  it should "return clients info" in {
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
