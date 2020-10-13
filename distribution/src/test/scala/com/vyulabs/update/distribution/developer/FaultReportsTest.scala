package com.vyulabs.update.distribution.developer

import java.io.File
import java.nio.file.Files
import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.ActorMaterializer
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.{ClientFaultReport, ServiceState}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import com.vyulabs.update.common.Common._
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.config.SslConfig
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.graphql.{DeveloperGraphqlContext, DeveloperGraphqlSchema}
import distribution.graphql.Graphql
import distribution.mongo.MongoDb
import sangria.macros.LiteralGraphQLStringContext

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}
import spray.json._

import Await._

class FaultReportsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer = ActorMaterializer()

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))
  implicit val filesLocker = new SmartFilesLocker()

  val mongo = new MongoDb("test")

  val collectionName = "faults"
  val collection = result(mongo.getOrCreateCollection[ClientFaultReport](collectionName), FiniteDuration(300, TimeUnit.SECONDS))

  val config = DeveloperDistributionConfig("Distribution", "instance1", 0, None, "distribution", None, "builder")

  val dir = new DeveloperDistributionDirectory(Files.createTempDirectory("test").toFile)
  val graphql = new Graphql(DeveloperGraphqlSchema.SchemaDefinition, DeveloperGraphqlContext(config, dir, mongo))

  val client1 = "client1"
  val client2 = "client2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  override def beforeAll() = {
    collection.drop().map(assert(_))
    assert(result(collection.dropItems(), FiniteDuration(3, TimeUnit.SECONDS)))
    assert(result(collection.insert(
      ClientFaultReport(client1, "fault1", Seq("fault.info", "core"),
        new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(), Seq.empty)), FiniteDuration(3, TimeUnit.SECONDS)))
    assert(result(collection.insert(
      ClientFaultReport(client2, "fault1", Seq("fault.info", "core1"),
        new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(), Seq.empty)), FiniteDuration(3, TimeUnit.SECONDS)))
    assert(result(collection.insert(
      ClientFaultReport(client1, "fault2", Seq("fault.info", "core"),
        new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(), Seq.empty)), FiniteDuration(3, TimeUnit.SECONDS)))
  }

  override protected def afterAll(): Unit = {
    collection.drop().map(assert(_))
  }

  it should "get last fault reports for specified client" in {
    val query =
      graphql"""
        query {
          faults (clientName: "client1", last: 1) {
            clientName
            reportDirectory
            serviceName
            instanceId
            reportFiles
          }
        }
      """
    val future = graphql.executeQuery(query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult(result)((OK,
      ("""{"data":{"faults":[""" +
       """{"clientName":"client1","reportDirectory":"fault2","serviceName":"serviceB","instanceId":"instance2","reportFiles":["fault.info","core"]}""" +
      """]}}""").parseJson))
  }

  it should "get last fault reports for specified service" in {
    val query =
      graphql"""
        query {
          faults (serviceName: "serviceA", last: 1) {
            clientName
            reportDirectory
            serviceName
            instanceId
            reportFiles
          }
        }
      """
    val future = graphql.executeQuery(query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult(result)((OK,
      ("""{"data":{"faults":[""" +
        """{"clientName":"client2","reportDirectory":"fault1","serviceName":"serviceA","instanceId":"instance1","reportFiles":["fault.info","core1"]}""" +
        """]}}""").parseJson))
  }

  it should "get fault reports for specified service in parameters" in {
    val query =
      graphql"""
        query FaultsQuery($$serviceName: String!) {
          faults (serviceName: $$serviceName) {
            clientName
            reportDirectory
            serviceName
            instanceId
            reportFiles
          }
        }
      """
    val future = graphql.executeQuery(query, None, variables = JsObject("serviceName" -> JsString("serviceB")))
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    assertResult(result)((OK,
      ("""{"data":{"faults":[""" +
        """{"clientName":"client1","reportDirectory":"fault2","serviceName":"serviceB","instanceId":"instance2","reportFiles":["fault.info","core"]}""" +
        """]}}""").parseJson))
  }
}
