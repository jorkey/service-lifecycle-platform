package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.info.{DesiredVersion}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.{Await, Awaitable, ExecutionContext}
import scala.concurrent.duration.FiniteDuration

class TestedVersionsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder", 5)

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)
  val collections = new DeveloperDatabaseCollections(mongo)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    /* TODO graphql
    result(mongo.dropDatabase())

    val desiredVersionsCollection = result(collections.DesiredVersion)
    val testedVersionsCollection = result(collections.TestedVersions)
    val installProfilesCollection = result(collections.InstallProfile)
    val clientDesiredVersionsCollection = result(collections.ClientDesiredVersion)
    val clientInfoCollection = result(collections.ClientInfo)

    result(desiredVersionsCollection.insert(
      DesiredVersions(Seq(
        DesiredVersion("service1", BuildVersion(1, 1, 2)),
        DesiredVersion("service2", BuildVersion(2, 1, 4)),
        DesiredVersion("service3", BuildVersion(3, 2, 1))))))

    result(installProfilesCollection.insert(
      InstallProfile("common", Set("service1", "service2"))))

    result(testedVersionsCollection.insert(
      TestedVersions("common", Map("service1" -> BuildVersion(1, 1, 1), "service2" -> BuildVersion(2, 1, 2)), Seq(TestSignature("test", new Date())))))

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("specific", Some("test")))))

    result(clientInfoCollection.insert(
      ClientInfo("client2", ClientConfig("common", Some("test")))))

    result(clientDesiredVersionsCollection.insert(
      ClientDesiredVersions("client2",
        DesiredVersions(Seq(
          DesiredVersion("service2", BuildVersion("client2", 1, 1, 1)))))))

    result(clientInfoCollection.insert(
      ClientInfo("client3", ClientConfig("common", Some("test")))))
     */
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions (client: "client1", merged: true) {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"No tested versions for profile specific","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(res)
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions (client: "client2", merged: true) {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Client required preliminary testing shouldn't have personal desired versions","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(res)
  }

  it should "return tested versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions (client: "client3", merged: true) {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.1"},{"serviceName":"service2","buildVersion":"2.1.2"}]}}}""").parseJson))(res)
  }
}
