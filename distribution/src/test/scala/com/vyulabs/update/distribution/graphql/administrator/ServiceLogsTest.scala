package com.vyulabs.update.distribution.graphql.administrator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class ServiceLogsTest extends TestEnvironment {
  behavior of "Service Logs Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def dbName = super.dbName + "-administrator"

  val graphqlContext = new GraphqlContext(UserInfo("administrator", UserRole.Administrator), workspace)

  it should "add/get service logs" in {
    addServiceLogLine("INFO", "unit1", "line1")
    addServiceLogLine("DEBUG", "unit1", "line2")
    addServiceLogLine("ERROR", "unit2", "line3")

    assertResult((OK,
      ("""{"data":{"serviceLogs":[""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"INFO","message":"line1"},"serviceName":"service1","directory":"dir"},""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"DEBUG","message":"line2"},"serviceName":"service1","directory":"dir"},""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"ERROR","message":"line3"},"serviceName":"service1","directory":"dir"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query ServiceLogs($$distribution: String!, $$service: String!, $$instance: String!, $$process: String!, $$directory: String!) {
          serviceLogs (distribution: $$distribution, service: $$service, instance: $$instance, process: $$process, directory: $$directory) {
            distributionName
            serviceName
            instanceId
            directory
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject(
        "distribution" -> JsString("test"),
        "service" -> JsString("service1"),
        "instance" -> JsString("instance1"),
        "process" -> JsString("process1"),
        "directory" -> JsString("dir"))))
    )
  }

  it should "subscribe service logs" in {
    setSequence("state.serviceLogs", 10)

    addServiceLogLine("INFO", "unit1", "line1")

    val response1 = subscribeServiceLogs(13)
    val source1 = response1.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input1 = source1.runWith(TestSink.probe[ServerSentEvent])

    addServiceLogLine("INFO", "unit1", "line2")
    addServiceLogLine("DEBUG", "unit2", "line3")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":13,"logLine":{"line":{"level":"DEBUG","message":"line3"}}}}}"""))

    addServiceLogLine("ERROR", "unit1", "line4")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":14,"logLine":{"line":{"level":"ERROR","message":"line4"}}}}}"""))

    val response2 = subscribeServiceLogs(11)
    val source2 = response2.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input2 = source2.runWith(TestSink.probe[ServerSentEvent])

    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":11,"logLine":{"line":{"level":"INFO","message":"line1"}}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":12,"logLine":{"line":{"level":"INFO","message":"line2"}}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":13,"logLine":{"line":{"level":"DEBUG","message":"line3"}}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":14,"logLine":{"line":{"level":"ERROR","message":"line4"}}}}}"""))
  }

  it should "subscribe task logs" in {
    setSequence("state.serviceLogs", 20)

    addTaskLogLine("INFO", "unit1", "line1")

    val response1 = subscribeTaskLogs(1)
    val source1 = response1.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input1 = source1.runWith(TestSink.probe[ServerSentEvent])

    addTaskLogLine("DEBUG", "unit2", "line2")

    input1.requestNext(ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":21,"logLine":{"line":{"level":"INFO","message":"line1"}}}}}"""))
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":22,"logLine":{"line":{"level":"DEBUG","message":"line2"}}}}}"""))

    addServiceLogLine("ERROR", "unit1", "line1")
    input1.expectNoMessage()

    addTaskLogLine("DEBUG", "unit1", "line3")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":24,"logLine":{"line":{"level":"DEBUG","message":"line3"}}}}}"""))
  }

  def addServiceLogLine(level: String, unit: String, message: String): Unit = {
    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation AddServiceLogs($$date: Date!, $$level: String!, $$unit: String!, $$message: String!) {
          addServiceLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            logs: [
              { date: $$date, level: $$level, unit: $$unit, message: $$message }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson, "level" -> JsString(level), "unit" -> JsString(unit), "message" -> JsString(message)))))
  }

  def subscribeServiceLogs(from: Long): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        subscription SubscribeServiceLogs($$from: Long!) {
          subscribeServiceLogs (
            distribution: "test",
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            from: $$from
          ) {
            sequence
            logLine {
              line {
                level
                message
              }
            }
          }
        }
      """, variables = JsObject("from" -> JsNumber(from))))
  }

  def addTaskLogLine(level: String, unit: String, message: String): Unit = {
    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation AddServiceLogs($$date: Date!, $$level: String!, $$unit: String!, $$message: String!) {
          addServiceLogs (
            service: "service2",
            task: "task1",
            instance: "instance2",
            process: "process2",
            directory: "dir",
            logs: [
              { date: $$date, level: $$level, unit: $$unit, message: $$message }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson, "level" -> JsString(level), "unit" -> JsString(unit), "message" -> JsString(message)))))
  }

  def subscribeTaskLogs(from: Long): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        subscription SubscribeTaskLogs($$from: Long!) {
          subscribeTaskLogs (
            task: "task1",
            from: $$from
          ) {
            sequence
            logLine {
              line {
                level
                message
              }
            }
          }
        }
      """, variables = JsObject("from" -> JsNumber(from))))
  }
}
