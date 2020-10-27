package com.vyulabs.update.distribution

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.DatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
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

  val dir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)
  val collections = new DatabaseCollections(mongo)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    result(mongo.dropDatabase())
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "set/get common desired versions" in {
    val graphqlContext = new GraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))

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
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"},{"serviceName":"service2","buildVersion":"2.1.4"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
             serviceName
             buildVersion
          }
        }
      """)))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"1.1.2"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions (services: ["service1"]) {
             serviceName
             buildVersion
          }
        }
      """)))
  }
}
