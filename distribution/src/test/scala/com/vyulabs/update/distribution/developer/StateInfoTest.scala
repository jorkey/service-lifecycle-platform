package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info.{ClientDesiredVersions, ClientServiceState, DesiredVersions, ServiceState}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb

import scala.concurrent.Await.result
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class StateInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "State Info Requests"

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
    val installedVersionsCollection = result(collections.ClientDesiredVersions, FiniteDuration(3, TimeUnit.SECONDS))
    val clientServiceStatesCollection = result(collections.ClientServiceState, FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))
    result(installedVersionsCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", Some("test")))), FiniteDuration(3, TimeUnit.SECONDS))

    result(installedVersionsCollection.insert(
      ClientDesiredVersions("client1",
        DesiredVersions(versions = Map("service1" -> BuildVersion(1, 1, 1), "service2" -> BuildVersion(2, 1, 3))))), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientServiceStatesCollection.insert(
      ClientServiceState("client1", "instance1", "service1", "directory1", ServiceState(version = Some(BuildVersion(1, 1, 0))))), FiniteDuration(3, TimeUnit.SECONDS))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  it should "return installed versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("user1", UserRole.Client))
    val query =
      graphql"""
        query {
          installedVersions (client: "client1") {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val future = graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"installedVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.1"},{"serviceName":"service2","buildVersion":"2.1.3"}]}}}""").parseJson))(result)
  }

  it should "return service state" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("user1", UserRole.Client))
    val query =
      graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instanceId
            state {
              version
            }
          }
        }
      """
    val future = graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instanceId":"instance1","state":{"version":"1.1.0"}}]}}""").parseJson))(result)
  }
}
