package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.info.{ClientDesiredVersions, DesiredVersions, ServiceVersion}
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

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class DesiredVersionsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Desired Versions Info Requests"

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
    result(mongo.dropDatabase())
    
    val desiredVersionsCollection = result(collections.DesiredVersions)
    val installProfilesCollection = result(collections.InstallProfile)
    val clientDesiredVersionsCollection = result(collections.ClientDesiredVersions)
    val clientInfoCollection = result(collections.ClientInfo)

    result(desiredVersionsCollection.drop())
    result(installProfilesCollection.drop())
    result(clientDesiredVersionsCollection.drop())
    result(clientInfoCollection.drop())

    result(desiredVersionsCollection.insert(
      DesiredVersions(Seq(
        ServiceVersion("service1", BuildVersion(1, 1, 2)),
        ServiceVersion("service2", BuildVersion(2, 1, 4)),
        ServiceVersion("service3", BuildVersion(3, 2, 1))))))

    result(installProfilesCollection.insert(
      InstallProfile("common", Set("service1", "service2"))))

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", None))))

    result(clientInfoCollection.insert(
      ClientInfo("client2", ClientConfig("common", None))))

    result(clientDesiredVersionsCollection.insert(
      ClientDesiredVersions("client2",
        DesiredVersions(Seq(ServiceVersion("service2", BuildVersion("client2", 1, 1, 1)))))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "return common desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.2"},""" +
      """{"serviceName":"service2","buildVersion":"2.1.4"},{"serviceName":"service3","buildVersion":"3.2.1"}]}}}""").parseJson))(res)
  }

  it should "return client own desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          clientDesiredVersions (client: "client2") {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}}""").parseJson))(res)
  }

  it should "return client without own versions merged desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          clientDesiredVersions (client: "client1", merged: true) {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}}""").parseJson))(res)
  }

  it should "return client with own versions merged desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          clientDesiredVersions (client: "client2", merged: true) {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query))
    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"},{"serviceName":"service1","buildVersion":"1.1.2"}]}}}""").parseJson))(res)
  }
}
