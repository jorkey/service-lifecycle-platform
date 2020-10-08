package com.vyulabs.update.distribution.developer

import java.util.Date
import java.util.concurrent.TimeUnit

import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.info.{ClientFaultReport, ServiceState}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import com.vyulabs.update.common.Common._
import distribution.mongo.{MongoDb, MongoDbCollection}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

import Await._
import spray.json._

class FaultReportsTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "AdaptationMeasure"

  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  val mongo = new MongoDb("test")

  val collection = result(mongo.getOrCreateCollection[ClientFaultReport](collectionName), FiniteDuration(3, TimeUnit.SECONDS))
  val collectionName = "faults"

  val client1 = "client1"
  val client2 = "client1"
  val client3 = "client1"

  val instance1 = "instance1"

  override def beforeAll() = {
    assert(result(collection.dropItems(), FiniteDuration(3, TimeUnit.SECONDS)))
    assert(result(collection.insert(
      ClientFaultReport(client1, "fault1", Seq("fault.info", "core"),
        new Date(), instance1, "runner", "runner", CommonServiceProfile, ServiceState(), Seq.empty)), FiniteDuration(3, TimeUnit.SECONDS)))
  }

  override protected def afterAll(): Unit = {
    collection.drop().map(assert(_))
  }

  it should "change status in depend of frames delivery delays" in {

  }

  /*
  "StartWars Schema" should {
    "correctly identify R2-D2 as the hero of the Star Wars Saga" in {
      val query =
        graphql"""
       query HeroNameQuery {
         hero {
           name
         }
       }
     """

      executeQuery(query) should be (parse(
        """
       {
         "data": {
           "hero": {
             "name": "R2-D2"
           }
         }
       }
     """).right.get)
    }

    "allow to fetch Han Solo using his ID provided through variables" in {
      val query =
        graphql"""
       query FetchSomeIDQuery($$humanId: String!) {
         human(id: $$humanId) {
           name
           friends {
             id
             name
           }
         }
       }
     """

      executeQuery(query, vars = Json.obj("humanId" -> Json.fromString("1002"))) should be (parse(
        """
       {
         "data": {
           "human": {
             "name": "Han Solo",
             "friends": [
               {
                 "id": "1000",
                 "name": "Luke Skywalker"
               },
               {
                 "id": "1003",
                 "name": "Leia Organa"
               },
               {
                 "id": "2001",
                 "name": "R2-D2"
               }
             ]
           }
         }
       }
      """).right.get)
    }
  }

  def executeQuery(query: Document, vars: Json = Json.obj()) = {
    val futureResult = Executor.execute(StarWarsSchema, query,
      variables = vars,
      userContext = new CharacterRepo,
      deferredResolver = DeferredResolver.fetchers(SchemaDefinition.characters))

    Await.result(futureResult, 10.seconds)
  }*/
}
