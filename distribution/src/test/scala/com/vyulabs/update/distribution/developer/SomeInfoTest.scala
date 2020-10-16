package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.ProfiledServiceName
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.IoUtils
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

class SomeInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getName)
  val graphql = new Graphql()

  override def beforeAll() = {
    IoUtils.writeJsonToFile(dir.getClientConfigFile("client1"), ClientConfig("common", None))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    mongo.dropDatabase().foreach(assert(_))
  }

  it should "return user info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, UserInfo("user1", UserRole.Client))
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
      ("""{"data":{"user":{"name":"user1","role":"Client"}}}""").parseJson))(result)
  }

  it should "return clients info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, UserInfo("admin", UserRole.Administrator))
    val query =
      graphql"""
        query {
          clientsInfo {
            name
            installProfile
            testClientMatch
          }
        }
      """
    val future = graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"user":{"name":"user1","role":"Client"}}}""").parseJson))(result)
  }

}
