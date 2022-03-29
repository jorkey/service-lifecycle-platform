package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.MongoClientSettings
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.utils.Utils
import org.bson.BsonDocument
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.Macros._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

class SequencedCollectionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Sequenced Collection"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val mongo = new MongoDb("SequencedCollectionTest", "mongodb://localhost:27017", false); result(mongo.dropDatabase())

  case class TestRecord(field1: String, field2: String)

  implicit val codecRegistry = fromRegistries(fromProviders(
    MongoClientSettings.getDefaultCodecRegistry(), classOf[TestRecord]))

  private def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  val testCollection = mongo.createCollection[BsonDocument]("test")
  val test = new SequencedCollection[TestRecord]("test", testCollection)

  it should "insert/modify/delete records" in {
    result(test.insert(TestRecord("v1", "v2")))
    result(test.update(Filters.eq("field1", "v1"), {
      record => record match {
        case Some(r) =>
          Some(TestRecord("v3", r.field2))
        case None =>
          None
      }
    }))
    assertResult(Seq(TestRecord("v3", "v2")))(result(test.find()))

    var concurrentUpdate: Future[Int] = null
    assertResult(1)(result(test.update(Filters.eq("field1", "v3"),
      record => {
        record match {
          case Some(r) =>
            assertResult(Seq(TestRecord("v3", "v2")))(result(test.find()))
            concurrentUpdate = test.update(Filters.eq("field1", "v3"),
              record => {
                record match {
                  case Some(r) =>
                    Some(TestRecord(r.field1, "v5"))
                  case None =>
                    None
                }
              }
            )
            Some(TestRecord(r.field1, "v4"))
          case None =>
            None
        }
      }
    )))
    val res = result(concurrentUpdate)
    assertResult(1)(res)
    assertResult(Seq(TestRecord("v3", "v5")))(result(test.find()))

    assertResult(1)(result(test.delete()))
    assertResult(Seq.empty)(result(test.find()))
  }
}
