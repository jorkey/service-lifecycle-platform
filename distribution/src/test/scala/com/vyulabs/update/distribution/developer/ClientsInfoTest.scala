package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info.VersionInfo
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.Await.result
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

class ClientsInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Client Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)
  val collections = new DeveloperDatabaseCollections(mongo)
  val graphql = new Graphql()

  override def beforeAll() = {
    val clientInfoCollection = result(collections.ClientInfo, FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", Some("test")))), FiniteDuration(3, TimeUnit.SECONDS))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  it should "return user info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("user1", UserRole.Client))
    val query =
      graphql"""
        query {
          userInfo {
            name
            role
          }
        }
      """
    val future = graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"userInfo":{"name":"user1","role":"Client"}}}""").parseJson))(result)
  }

  it should "return clients info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
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
      """
    val future = graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"clientsInfo":[{"clientName":"client1","clientConfig":{"installProfile":"common","testClientMatch":"test"}}]}}""").parseJson))(result)
  }
}
