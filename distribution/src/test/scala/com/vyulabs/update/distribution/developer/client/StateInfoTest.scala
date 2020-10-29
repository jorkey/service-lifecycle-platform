package com.vyulabs.update.distribution.developer.client

import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.alpakka.mongodb.scaladsl.{MongoSink, MongoSource}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.{MongoClient, MongoClients}
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.info.{ClientDesiredVersions, DesiredVersion, TestSignature, TestedVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.version.BuildVersion
import distribution.config.VersionHistoryConfig
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import org.bson.codecs.configuration.CodecRegistries.fromCodecs
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext}

class StateInfoTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Tested Versions Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val log = LoggerFactory.getLogger(this.getClass)

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val versionHistoryConfig = VersionHistoryConfig(5)

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val mongo = new MongoDb(getClass.getSimpleName)

  result(mongo.dropDatabase())

  val collections = new DeveloperDatabaseCollections(mongo, "self-instance", "builder", 1)
  val graphql = new Graphql()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(3, TimeUnit.SECONDS))

  override protected def afterAll(): Unit = {
    dir.drop()
    //result(mongo.dropDatabase())
  }

  it should "qwe" in {

    import org.bson.codecs.configuration.CodecRegistries.{fromRegistries}
    import org.bson.codecs.configuration.CodecRegistries.fromProviders
    import org.mongodb.scala.bson.codecs.Macros._

    case class Number(id: Int, date: Date, l: Option[String], branches: Seq[String])
    val codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), fromProviders(classOf[Number]))

    val client = MongoClients.create("mongodb://localhost:27017")
    val db = client.getDatabase("MongoSourceSpec")
    val numbersColl = db
      .getCollection("numbers2", classOf[Number])
      .withCodecRegistry(codecRegistry)

    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()

    val source = Source.single(Number(1, new Date(), Some("qwe"), Seq("asdf")))
    result(source.runWith(MongoSink.insertOne(numbersColl))(mat))

    val sink = Sink.ignore
    result(MongoSource[Number](numbersColl.find()).runWith(sink)(mat))
  }

  it should "set installed versions" in {
    val graphqlContext1 = new DeveloperGraphqlContext(versionHistoryConfig, dir, collections, UserInfo("client1", UserRole.Client))

    assertResult((OK,
      ("""{"data":{"installedVersions":true}}""").parseJson))(
      result(graphql.executeQuery(DeveloperGraphqlSchema.ClientSchemaDefinition, graphqlContext1, graphql"""
        mutation {
          installedVersions (
            versions: [
               { serviceName: "service1", buildVersion: "1.1.1" },
               { serviceName: "service2", buildVersion: "2.1.1" }
            ]
          )
        }
      """)))

    Thread.sleep(2000)

    result(collections.ClientInstalledVersions.map(_.find().map(assertResult(_)(ClientDesiredVersions("client1",
      Seq(DesiredVersion("service1", BuildVersion(1, 1, 1)), DesiredVersion("service2", BuildVersion(2, 1, 1))))))))
    //result(collections.ClientInstalledVersions.map(_.dropItems()))
  }
}
