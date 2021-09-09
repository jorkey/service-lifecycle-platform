package com.vyulabs.update.distribution.graphql.state

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class ServiceLogsTest extends TestEnvironment {
  behavior of "Service Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def dbName = super.dbName + "-administrator"

  it should "add/get service logs" in {
    addLogLine("INFO", "unit1", "line1")
    addLogLine("DEBUG", "unit1", "line2")
    addLogLine("ERROR", "unit2", "line3")

    assertResult((OK,
      ("""{"data":{"logs":[""" +
       """{"line":{"level":"INFO","message":"line1"}},""" +
       """{"line":{"level":"DEBUG","message":"line2"}},""" +
       """{"line":{"level":"ERROR","message":"line3"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query logs($$service: String!, $$instance: String!, $$process: String!, $$directory: String!) {
          logs (service: $$service, instance: $$instance, process: $$process, directory: $$directory) {
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject(
        "service" -> JsString("service1"),
        "instance" -> JsString("instance1"),
        "process" -> JsString("process1"),
        "directory" -> JsString("dir"))))
    )
  }

  it should "subscribe logs" in {
    setSequence("state.serviceLogs", 10)

    addLogLine("INFO", "unit1", "line1")

    val response1 = subscribeLogs(13)
    val source1 = response1.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input1 = source1.runWith(TestSink.probe[ServerSentEvent])

    addLogLine("INFO", "unit1", "line2")
    addLogLine("DEBUG", "unit2", "line3")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":13,"line":{"level":"DEBUG","message":"line3"}}}}"""))

    addLogLine("ERROR", "unit1", "line4")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":14,"line":{"level":"ERROR","message":"line4"}}}}"""))

    val response2 = subscribeLogs(11)
    val source2 = response2.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input2 = source2.runWith(TestSink.probe[ServerSentEvent])

    input2.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":11,"line":{"level":"INFO","message":"line1"}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":12,"line":{"level":"INFO","message":"line2"}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":13,"line":{"level":"DEBUG","message":"line3"}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":14,"line":{"level":"ERROR","message":"line4"}}}}"""))
  }

  it should "subscribe task logs" in {
    setSequence("state.serviceLogs", 20)

    addTaskLogLine("INFO", "unit1", "line1")

    val response1 = subscribeTaskLogs(1)
    val source1 = response1.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input1 = source1.runWith(TestSink.probe[ServerSentEvent])

    addTaskLogLine("DEBUG", "unit2", "line2")

    input1.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":21,"line":{"level":"INFO","message":"line1"}}}}"""))
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":22,"line":{"level":"DEBUG","message":"line2"}}}}"""))

    addLogLine("ERROR", "unit1", "line1")
    input1.expectNoMessage()

    addTaskLogLine("DEBUG", "unit1", "line3")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeLogs":{"sequence":24,"line":{"level":"DEBUG","message":"line3"}}}}"""))
  }

  def addLogLine(level: String, unit: String, message: String): Unit = {
    assertResult((OK,
      ("""{"data":{"addLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, updaterContext, graphql"""
        mutation AddLogs($$time: Date!, $$level: String!, $$unit: String!, $$message: String!) {
          addLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            logs: [
              { time: $$time, level: $$level, unit: $$unit, message: $$message }
            ]
          )
        }
      """, variables = JsObject("time" -> new Date().toJson, "level" -> JsString(level), "unit" -> JsString(unit), "message" -> JsString(message)))))
  }

  def subscribeLogs(from: Long): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQueryToSSE(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        subscription SubscribeLogs($$from: Long!) {
          subscribeLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            from: $$from
          ) {
            sequence
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject("from" -> JsNumber(from))))
  }

  def addTaskLogLine(level: String, unit: String, message: String): Unit = {
    assertResult((OK,
      ("""{"data":{"addLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, updaterContext, graphql"""
        mutation AddLogs($$time: Date!, $$level: String!, $$unit: String!, $$message: String!) {
          addLogs (
            service: "service2",
            task: "task1",
            instance: "instance2",
            process: "process2",
            directory: "dir",
            logs: [
              { time: $$time, level: $$level, unit: $$unit, message: $$message }
            ]
          )
        }
      """, variables = JsObject("time" -> new Date().toJson, "level" -> JsString(level), "unit" -> JsString(unit), "message" -> JsString(message)))))
  }

  def subscribeTaskLogs(from: Long): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQueryToSSE(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        subscription SubscribeLogs($$from: Long!) {
          subscribeLogs (
            task: "task1",
            from: $$from
          ) {
            sequence
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject("from" -> JsNumber(from))))
  }
}
