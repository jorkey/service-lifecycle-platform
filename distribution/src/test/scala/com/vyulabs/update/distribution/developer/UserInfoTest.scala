package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

class UserInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb("test")

  val graphql = new Graphql(DeveloperGraphqlSchema.SchemaDefinition)

  it should "return user info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, Some(UserInfo("user1", UserRole.Client)))
    val query =
      graphql"""
        query {
          user {
            name
            role
          }
        }
      """
    val future = graphql.executeQuery(graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":{"user":{"name":"user1","role":"Client"}}}""").parseJson))(result)
  }

  it should "return error when user is not logged in" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, mongo, None)
    val query =
      graphql"""
        query {
          user {
            name
            role
          }
        }
      """
    val future = graphql.executeQuery(graphqlContext, query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"You are not logged in","path":["user"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(result)
  }
}
