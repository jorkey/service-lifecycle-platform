package com.vyulabs.update.distribution.developer

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.{IoUtils, Utils}
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

class VersionsInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Version Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val ownServicesDir = Files.createTempDirectory("test").toFile

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)
  val collections = new DeveloperDatabaseCollections(mongo)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    IoUtils.writeServiceVersion(ownServicesDir, Common.DistributionServiceName, BuildVersion(1, 2, 3))

    val clientInfoCollection = result(collections.ClientInfo)

    result(clientInfoCollection.drop())

    result(clientInfoCollection.insert(ClientInfo("client1", ClientConfig("common", Some("test")))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    IoUtils.deleteFileRecursively(ownServicesDir)
    result(mongo.dropDatabase())
  }

  it should "return own service info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"ownServiceVersion":"1.2.3"}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query DistributionVersionQuery($$directory: String!) {
          ownServiceVersion (service: "distribution", directory: $$directory)
        }
      """
      ,
      None, variables = JsObject("directory" -> JsString(ownServicesDir.toString)))))
  }

  it should "return version info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1", version: "1.1.2") {
            version
            buildInfo {
              author
            }
          }
        }
      """
    )))
  }

  it should "add/get versions info" in {
    import com.vyulabs.update.utils.Utils.DateJson._
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"addVersionInfo":{"version":"1.1.2"}}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation AddVersionInfo($$date: Date!) {
                    addVersionInfo (
                      service: "service1",
                      version: "1.1.2",
                      buildInfo: {
                        author: "author1",
                        branches: [ "master" ]
                        date: $$date
                      }) {
                      version
                    }
                  }
                """
        ,
        variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"addVersionInfo":{"version":"1.1.3"}}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation AddVersionInfo($$date: Date!) {
                    addVersionInfo (
                      service: "service1",
                      version: "1.1.3",
                      buildInfo: {
                        author: "author1",
                        branches: [ "master" ]
                        date: $$date
                      }) {
                      version
                    }
                  }
                """
        ,
        variables = JsObject("date" -> new Date().toJson))))

    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"1.1.2","buildInfo":{"author":"author1"}},{"version":"1.1.3","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
        query {
          versionsInfo (service: "service1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
      ))
    )
  }

  it should "return client versions info" in {
    val graphqlContext = DeveloperGraphqlContext(config, dir, collections, UserInfo("admin", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"client1-1.1.0","buildInfo":{"author":"author2"}},{"version":"client1-1.1.1","buildInfo":{"author":"author2"}}]}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1", client: "client1") {
            version
            buildInfo {
              author
            }
          }
        }
      """)))
  }
}
