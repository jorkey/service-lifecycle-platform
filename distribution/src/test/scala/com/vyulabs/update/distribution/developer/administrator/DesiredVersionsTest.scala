package com.vyulabs.update.distribution.developer.administrator

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.{Graphql, GraphqlSchema}
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

    val installProfileCollection = result(collections.InstallProfile)
    val clientInfoCollection = result(collections.ClientInfo)

    result(installProfileCollection.insert(InstallProfile("common", Set("service1", "service2"))))
    result(clientInfoCollection.insert(ClientInfo("client2", ClientConfig("common", None))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "set/get client own desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          clientDesiredVersions (
            client: "client2",
            versions: [
               { serviceName: "service2", buildVersion: "client2-1.1.1" }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (client: "client2") {
             serviceName
             buildVersion
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          clientDesiredVersions (
            client: "client2",
            versions: []
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (client: "client2") {
             serviceName
             buildVersion
          }
        }
      """)))
  }

  it should "return client merged desired versions" in {
    val graphqlContext = new DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))

    assertResult((OK,
      ("""{"data":{"desiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          desiredVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.2"},
               { serviceName: "service2", buildVersion: "2.1.4"}
            ]
          )
        }
      """)))


    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":true}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          clientDesiredVersions (
            client: "client2",
            versions: [
               { serviceName: "service2", buildVersion: "client2-1.1.1" }
            ]
          )
        }
      """)))

    assertResult((OK,
      ("""{"data":{"clientDesiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"client2-1.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          clientDesiredVersions (client: "client2", merged: true) {
             serviceName
             buildVersion
          }
        }
      """))
    )

    result(collections.DesiredVersions.map(_.dropItems()))
    result(collections.ClientDesiredVersions.map(_.dropItems()))
  }
}
