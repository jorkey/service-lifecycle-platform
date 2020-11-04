package com.vyulabs.update.distribution.graphql.client

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{ClientDesiredVersions, ClientServiceState, DesiredVersion, ServiceState, TestSignature, TestedVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.DatabaseCollections

class StateInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val versionHistoryConfig = VersionHistoryConfig(5)

  val dir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, "self-instance", Some("builder"), 1)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override protected def beforeAll(): Unit = {
    result(collections.Developer_ClientsInfo.map(_.insert(ClientInfo("client1", ClientConfig("common", Some("test"))))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "set tested versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"testedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          testedVersions (
            versions: [
              { serviceName: "service1", buildVersion: "1.1.2" },
              { serviceName: "service2", buildVersion: "2.1.2" }
            ]
          )
        }
      """)))

    val date = new Date()
    result(collections.State_TestedVersions.map(v => result(v.find().map(_.map(v => TestedVersions(v.profileName, v.versions, v.signatures.map(s => TestSignature(s.clientName, date)))))
      .map(assertResult(_)(Seq(TestedVersions("common",
        Seq(DesiredVersion("service1", BuildVersion(1, 1, 2)), DesiredVersion("service2", BuildVersion(2, 1, 2))), Seq(TestSignature("client1", date)))))))))
    result(collections.State_TestedVersions.map(_.dropItems()))
  }

  it should "set installed versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"installedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        mutation {
          installedVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.1" },
               { serviceName: "service2", buildVersion: "2.1.1" }
            ]
          )
        }
      """)))

    result(collections.Client_DesiredVersions.map(v => result(v.find().map(assertResult(_)(Seq(ClientDesiredVersions(Some("client1"),
      Seq(DesiredVersion("service1", BuildVersion(1, 1, 1)), DesiredVersion("service2", BuildVersion(2, 1, 1))))))))))
    result(collections.Client_DesiredVersions.map(_.dropItems()))
  }

  it should "set services state" in {
    val graphqlContext1 = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Client))
    assertResult((OK,
      ("""{"data":{"servicesState":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation ServicesState($$date: Date!) {
          servicesState (
            state: [
              { instanceId: "instance1", serviceName: "service1", directory: "dir",
                  state: { date: $$date, version: "1.2.3" }
              }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson))))

    val graphqlContext2 = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":{"servicesState":[{"instanceId":"instance1","state":{"version":"1.2.3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext2, graphql"""
        query {
          servicesState (client: "client1", service: "service1") {
            instanceId
            state {
              version
            }
          }
        }
      """))
    )

    result(collections.State_ServiceStates.map(_.dropItems()))
  }
}
