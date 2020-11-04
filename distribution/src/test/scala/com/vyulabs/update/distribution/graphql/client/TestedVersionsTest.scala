package com.vyulabs.update.distribution.graphql.client

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{InstalledDesiredVersions, DesiredVersion, TestSignature, TestedDesiredVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.DatabaseCollections
import distribution.config.VersionHistoryConfig
import distribution.graphql.{Graphql, GraphqlContext, GraphqlSchema}
import distribution.mongo.MongoDb
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class TestedVersionsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val versionHistoryConfig = VersionHistoryConfig(5)

  val dir = new DistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName); result(mongo.dropDatabase())
  val collections = new DatabaseCollections(mongo, "self-instance", Some("builder"), 100)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override def beforeAll() = {
    val installProfileCollection = result(collections.Developer_ClientsProfiles)
    val clientInfoCollection = result(collections.Developer_ClientsInfo)

    result(installProfileCollection.insert(ClientProfile("common", Set("service1", "service2"))))

    result(clientInfoCollection.insert(ClientInfo("test-client", ClientConfig("common", None))))
    result(clientInfoCollection.insert(ClientInfo("client1", ClientConfig("common", Some("test-client")))))
  }

  override protected def afterAll(): Unit = {
    dir.drop()
    result(mongo.dropDatabase())
  }

  it should "set/get tested versions" in {
    val graphqlContext1 = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("test-client", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"testedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation {
          testedVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.1" },
               { serviceName: "service2", buildVersion: "2.1.1" }
            ]
          )
        }
      """)))

    val graphqlContext2 = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"desiredVersions":[{"serviceName":"service1","buildVersion":"1.1.1"},{"serviceName":"service2","buildVersion":"2.1.1"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext2, graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))

    result(collections.State_TestedVersions.map(_.dropItems()))
  }

  it should "return error if no tested versions for the client's profile" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Administrator))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Desired versions for profile common are not tested by anyone","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))
  }

  it should "return error if client required preliminary testing has personal desired versions" in {
    val graphqlContext = new GraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Administrator))
    result(collections.State_TestedVersions.map(_.insert(TestedDesiredVersions("common", Seq(DesiredVersion("service1", BuildVersion(1, 1, 0))), Seq(TestSignature("test-client", new Date()))))))
    result(collections.State_InstalledDesiredVersions.map(_.insert(InstalledDesiredVersions("client1", Seq(DesiredVersion("service1", BuildVersion("client1", 1, 1)))))))
    assertResult((OK,
      ("""{"data":null,"errors":[{"message":"Client required preliminary testing shouldn't have personal desired versions","path":["desiredVersions"],"locations":[{"column":11,"line":3}]}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext,
        graphql"""
        query {
          desiredVersions {
            serviceName
            buildVersion
          }
        }
      """)))
    result(collections.Client_DesiredVersions.map(_.dropItems()))
  }
}
