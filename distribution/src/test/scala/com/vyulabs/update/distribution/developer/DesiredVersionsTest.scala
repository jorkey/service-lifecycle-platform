package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.info.{BuildVersionInfo, ClientDesiredVersions, DesiredVersions, VersionInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
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

class DesiredVersionsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Version Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)
  val graphql = new Graphql()

  override def beforeAll() = {
    val desiredVersionsCollection = result(mongo.getOrCreateCollection[DesiredVersions](), FiniteDuration(3, TimeUnit.SECONDS))
    val installProfilesCollection = result(mongo.getOrCreateCollection[InstallProfile](), FiniteDuration(3, TimeUnit.SECONDS))
    val clientDesiredVersionsCollection = result(mongo.getOrCreateCollection[ClientDesiredVersions](), FiniteDuration(3, TimeUnit.SECONDS))
    val clientInfoCollection = result(mongo.getOrCreateCollection[ClientInfo](), FiniteDuration(3, TimeUnit.SECONDS))

    desiredVersionsCollection.drop().foreach(assert(_))
    installProfilesCollection.drop().foreach(assert(_))
    clientDesiredVersionsCollection.drop().foreach(assert(_))
    clientInfoCollection.drop().foreach(assert(_))

    assert(result(desiredVersionsCollection.insert(
      DesiredVersions(versions = Map("service1" -> BuildVersion(1, 1, 2), "service2" -> BuildVersion(2, 1, 4), "service3" -> BuildVersion(3, 2, 1)))), FiniteDuration(3, TimeUnit.SECONDS)))

    assert(result(installProfilesCollection.insert(
      InstallProfile("common", Set("service1", "service2", "service3"))), FiniteDuration(3, TimeUnit.SECONDS)))

    assert(result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", None))), FiniteDuration(3, TimeUnit.SECONDS)))

    assert(result(clientDesiredVersionsCollection.insert(
      ClientDesiredVersions("client1",
        DesiredVersions(versions = Map("service2" -> BuildVersion("client1", 1, 1, 1))))), FiniteDuration(3, TimeUnit.SECONDS)))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    mongo.dropDatabase().foreach(assert(_))
  }

  it should "return common desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, UserInfo("admin", UserRole.Administrator))
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
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query), FiniteDuration.apply(3, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.2"},""" +
      """{"serviceName":"service2","buildVersion":"2.1.4"},{"serviceName":"service3","buildVersion":"3.2.1"}]}}}""").parseJson))(res)
  }

  it should "return client desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions (client: "client1") {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query), FiniteDuration.apply(3, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client1-1.1.1"}]}}}""").parseJson))(res)
  }

  it should "return merged client desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, UserInfo("admin", UserRole.Administrator))
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
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query), FiniteDuration.apply(3, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client1-1.1.1"},{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service3","buildVersion":"3.2.1"}]}}}""").parseJson))(res)
  }
}
