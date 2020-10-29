package com.vyulabs.update.distribution.developer.administrator

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.{Graphql, GraphqlContext}
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class ClientsInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Client Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val versionHistoryConfig = VersionHistoryConfig(5)

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)

  result(mongo.dropDatabase())

  val collections = new DeveloperDatabaseCollections(mongo, "self-instance", "builder", 100)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    val clientInfoCollection = result(collections.ClientInfo)

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", Some("test")))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "return clients info" in {
    val graphqlContext = new DeveloperGraphqlContext(versionHistoryConfig, dir, collections, UserInfo("admin", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"clientsInfo":[{"clientName":"client1","clientConfig":{"installProfile":"common","testClientMatch":"test"}}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
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
