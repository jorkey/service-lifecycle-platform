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

class DesiredVersionsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Desired Versions Info Requests"

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
    val desiredVersionsCollection = result(collections.DesiredVersions, FiniteDuration(3, TimeUnit.SECONDS))
    val installProfilesCollection = result(collections.InstallProfile, FiniteDuration(3, TimeUnit.SECONDS))
    val clientDesiredVersionsCollection = result(collections.ClientDesiredVersions, FiniteDuration(3, TimeUnit.SECONDS))
    val clientInfoCollection = result(collections.ClientInfo, FiniteDuration(3, TimeUnit.SECONDS))

    result(desiredVersionsCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))
    result(installProfilesCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))
    result(clientDesiredVersionsCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))
    result(clientInfoCollection.drop(), FiniteDuration(3, TimeUnit.SECONDS))

    result(desiredVersionsCollection.insert(
      DesiredVersions(versions = Map("service1" -> BuildVersion(1, 1, 2), "service2" -> BuildVersion(2, 1, 4), "service3" -> BuildVersion(3, 2, 1)))), FiniteDuration(3, TimeUnit.SECONDS))

    result(installProfilesCollection.insert(
      InstallProfile("common", Set("service1", "service2"))), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.insert(
      ClientInfo("client1", ClientConfig("common", None))), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientInfoCollection.insert(
      ClientInfo("client2", ClientConfig("common", None))), FiniteDuration(3, TimeUnit.SECONDS))

    result(clientDesiredVersionsCollection.insert(
      ClientDesiredVersions("client2",
        DesiredVersions(versions = Map("service2" -> BuildVersion("client2", 1, 1, 1))))), FiniteDuration(3, TimeUnit.SECONDS))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
  }

  it should "return common desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
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

  it should "return client own desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          desiredVersions (client: "client2") {
            versions {
               serviceName
               buildVersion
            }
          }
        }
      """
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query), FiniteDuration.apply(3, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}}""").parseJson))(res)
  }

  it should "return client without own versions merged desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
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
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}}""").parseJson))(res)
  }

  it should "return client with own versions merged desired versions" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
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
    val res = result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query), FiniteDuration.apply(3, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"desiredVersions":{"versions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"},{"serviceName":"service1","buildVersion":"1.1.2"}]}}}""").parseJson))(res)
  }
}
