package com.vyulabs.update.distribution.developer

import java.util.Date
import java.util.concurrent.TimeUnit

import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.{ClientFaultReport, ServiceState}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import com.vyulabs.update.common.Common._
import distribution.developer.graphql.DeveloperGraphqlSchema
import distribution.graphql.{Graphql, GraphqlContext}
import distribution.mongo.MongoDb
import sangria.macros.LiteralGraphQLStringContext

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

import spray.json._

import Await._

class FaultReportsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  val mongo = new MongoDb("test")

  val collectionName = "faults"
  val collection = result(mongo.getOrCreateCollection[ClientFaultReport](collectionName), FiniteDuration(3, TimeUnit.SECONDS))

  val graphql = new Graphql(DeveloperGraphqlSchema.SchemaDefinition, GraphqlContext(mongo))

  val client1 = "client1"
  val client2 = "client1"
  val client3 = "client1"

  val instance1 = "instance1"

  override def beforeAll() = {
    collection.drop().map(assert(_))
    assert(result(collection.dropItems(), FiniteDuration(3, TimeUnit.SECONDS)))
    assert(result(collection.insert(
      ClientFaultReport(client1, "fault1", Seq("fault.info", "core"),
        new Date(), instance1, "runner", "runner", CommonServiceProfile, ServiceState(), Seq.empty)), FiniteDuration(3, TimeUnit.SECONDS)))
  }

  override protected def afterAll(): Unit = {
    collection.drop().map(assert(_))
  }

  it should "change status in depend of frames delivery delays" in {
    val query =
      graphql"""
        query ClientFaultsQuery {
          faults (client: "client1", service: "runner") {
            date
            instanceId
          }
        }
      """
    val future = graphql.executeQuery(query)
    val result = Await.result(future, FiniteDuration.apply(1, TimeUnit.SECONDS))
    println(result)
  }
}
