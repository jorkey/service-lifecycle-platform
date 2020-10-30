package com.vyulabs.update.distribution

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.utils.Utils.DateJson._
import com.vyulabs.update.version.BuildVersion
import distribution.DatabaseCollections
import distribution.config.{DistributionConfig, VersionHistoryConfig}
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
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

  val ownServicesDir = Files.createTempDirectory("test").toFile

  val dir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo)
  val graphql = new Graphql()

  val graphqlContext = GraphqlContext(VersionHistoryConfig(3), dir, collections, UserInfo("admin", UserRole.Administrator))

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    dir.drop()
    IoUtils.deleteFileRecursively(ownServicesDir)
    result(mongo.dropDatabase())
  }

  it should "return user info" in {
    assertResult((OK,
      ("""{"data":{"userInfo":{"name":"admin","role":"Administrator"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          userInfo {
            name
            role
          }
        }
      """))
    )
  }

  it should "return own service info" in {
    IoUtils.writeServiceVersion(ownServicesDir, Common.DistributionServiceName, BuildVersion(1, 2, 3))
    assertResult((OK,
      ("""{"data":{"ownServiceVersion":"1.2.3"}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query DistributionVersionQuery($$directory: String!) {
          ownServiceVersion (service: "distribution", directory: $$directory)
        }
      """
      ,
      None, variables = JsObject("directory" -> JsString(ownServicesDir.toString)))))
  }

  it should "add/get version info" in {
    addVersionInfo("service1", BuildVersion(1, 1, 1))

    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"1.1.1","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1", version: "1.1.1") {
            version
            buildInfo {
              author
            }
          }
        }
      """
    )))

    removeVersions()
  }

  it should "get versions info" in {
    addVersionInfo("service1", BuildVersion(1, 1, 1))
    addVersionInfo("service1", BuildVersion(1, 1, 2))

    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"1.1.1","buildInfo":{"author":"author1"}},{"version":"1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1") {
            version
            buildInfo {
              author
            }
          }
        }
      """))
    )

    removeVersions()
  }

  it should "add/get client version info" in {
    addVersionInfo("service1", BuildVersion("client1", 1, 1, 2))

    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"client1-1.1.2","buildInfo":{"author":"author1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1", client: "client1") {
            version
            buildInfo {
              author
            }
          }
        }
      """)))

    removeVersions()
  }

  it should "remove obsolete versions" in {
    addVersionInfo("service1", BuildVersion(1))
    addVersionInfo("service1", BuildVersion(2))
    addVersionInfo("service1", BuildVersion(3))
    addVersionInfo("service1", BuildVersion(4))
    addVersionInfo("service1", BuildVersion(5))

    assertResult((OK,
      ("""{"data":{"versionsInfo":[{"version":"3"},{"version":"4"},{"version":"5"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          versionsInfo (service: "service1") {
            version
          }
        }
      """)))

    removeVersions()
  }

  def addVersionInfo(serviceName: ServiceName, version: BuildVersion): Unit = {
    assertResult((OK,
      (s"""{"data":{"addVersionInfo":{"version":"${version.toString}"}}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext,
        graphql"""
                  mutation AddVersionInfo($$service: String!, $$version: BuildVersion!, $$date: Date!) {
                    addVersionInfo (
                      service: $$service,
                      version: $$version,
                      buildInfo: {
                        author: "author1",
                        branches: [ "master" ]
                        date: $$date
                      }) {
                      version
                    }
                  }
                """,
        variables = JsObject("service" -> JsString(serviceName), "version" -> version.toJson, "date" -> new Date().toJson))))
  }

  def removeVersions(): Unit = {
    result(collections.VersionInfo.map(_.dropItems()))
  }
}
