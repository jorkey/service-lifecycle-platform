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
import com.vyulabs.update.common.utils.Utils.DateJson._
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

    addServiceLogLine("INFO", "line1")
    addServiceLogLine("DEBUG", "line2")
    addServiceLogLine("ERROR", "line3")

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
    val response1 = subscribeServiceLogs(2)
    val source1 = response1.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input1 = source1.runWith(TestSink.probe[ServerSentEvent])

    addServiceLogLine("INFO", "line1")
    addServiceLogLine("DEBUG", "line2")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":2,"logLine":{"line":{"level":"DEBUG","message":"line2"}}}}}"""))

    addServiceLogLine("ERROR", "line3")
    input1.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":3,"logLine":{"line":{"level":"ERROR","message":"line3"}}}}}"""))

    val response2 = subscribeServiceLogs(1)
    val source2 = response2.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val input2 = source2.runWith(TestSink.probe[ServerSentEvent])

    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":1,"logLine":{"line":{"level":"INFO","message":"line1"}}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":2,"logLine":{"line":{"level":"DEBUG","message":"line2"}}}}}"""))
    input2.requestNext(ServerSentEvent("""{"data":{"subscribeServiceLogs":{"sequence":3,"logLine":{"line":{"level":"ERROR","message":"line3"}}}}}"""))
  }

  def addServiceLogLine(level: String, message: String): Unit = {
    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation AddServiceLogs($$date: Date!, $$level: String!, $$message: String!) {
          addServiceLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            logs: [
              { date: $$date, level: $$level, message: $$message }
            ]
          )
        }
      """, variables = JsObject("date" -> new Date().toJson, "level" -> JsString(level), "message" -> JsString(message)))))
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

  def clear(): Unit = {
    result(collections.State_ServiceLogs.drop())
    result(result(collections.Sequences).drop())
  }
}
