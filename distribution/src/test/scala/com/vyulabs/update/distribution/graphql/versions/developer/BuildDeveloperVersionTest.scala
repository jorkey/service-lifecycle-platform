package com.vyulabs.update.distribution.graphql.versions.developer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json.{JsObject, JsString, _}

import java.io.File
import scala.concurrent.ExecutionContext

class BuildDeveloperVersionTest extends TestEnvironment {
  behavior of "Build Developer Version"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  val dummyBuilder = new File(builderDirectory, "builder.sh")
  IoUtils.writeBytesToFile(dummyBuilder,
    ("echo \"2021-03-23 19:12:19.146 INFO Unit1 - Builder started\"\n" +
    "sleep 1\n" +
    "echo \"2021-03-23 19:12:20.150 INFO Unit2 - Builder continued\"\n" +
    "sleep 1\n" +
    "echo \"2021-03-23 19:12:21.100 INFO Unit3 - Builder finished\"").getBytes)

  it should "build developer version" in {
    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext, graphql"""
        mutation {
          buildDeveloperVersion (service: "service1", version: { build: [1,1,1] }, sources: [], comment: "Test version")
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("buildDeveloperVersion").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":1,"line":{"level":"INFO","message":"`Build developer version 1.1.1 of service service1` started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":2,"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=developer, sources=[], comment=Test version) in directory ${builderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":5,"line":{"level":"INFO","message":"Builder started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":6,"line":{"level":"INFO","message":"Builder continued"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":7,"line":{"level":"INFO","message":"Builder finished"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":9,"line":{"level":"","message":"Builder process terminated with status 0"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":10,"line":{"level":"INFO","message":"Upload developer desired versions List(DeveloperDesiredVersionDelta(service1,Some(test-1.1.1)))"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":11,"line":{"level":"INFO","message":"`Build developer version 1.1.1 of service service1` finished successfully"}}}}"""))
    logInput.expectComplete()
  }

  it should "cancel of building developer version" in {
    setSequence("state.serviceLogs", 10)

    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext, graphql"""
        mutation {
          buildDeveloperVersion (service: "service1", version: { build: [1,1,1] }, sources: [], comment: "Test version")
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("buildDeveloperVersion").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":11,"line":{"level":"INFO","message":"`Build developer version 1.1.1 of service service1` started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":12,"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=developer, sources=[], comment=Test version) in directory ${builderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":15,"line":{"level":"INFO","message":"Builder started"}}}}"""))

    assertResult((OK, ("""{"data":{"cancelTask":true}}""").parseJson))(result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext, graphql"""
        mutation CancelTask($$task: String!) {
          cancelTask (task: $$task)
        }
      """, variables = JsObject("task" -> JsString(task)))))

    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    logInput.expectComplete()
  }

  it should "run builder" in {
    setSequence("state.serviceLogs", 20)

    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, distributionContext, graphql"""
        mutation {
          runBuilder (arguments: ["buildDeveloperVersion", "distribution=test", "service=service1", "version=1.1.1", "author=admin", "sources=[]"])
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("runBuilder").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":21,"line":{"level":"INFO","message":"`Run local builder by remote distribution` started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":22,"line":{"level":"INFO","message":"Start command /bin/sh with arguments Vector(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=admin, sources=[]) in directory ${builderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":25,"line":{"level":"INFO","message":"Builder started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":26,"line":{"level":"INFO","message":"Builder continued"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":27,"line":{"level":"INFO","message":"Builder finished"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":29,"line":{"level":"","message":"Builder process terminated with status 0"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":30,"line":{"level":"INFO","message":"`Run local builder by remote distribution` finished successfully"}}}}"""))
    logInput.expectComplete()
  }

  def subscribeTaskLogs(task: TaskId): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQueryToSSE(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        subscription SubscribeTaskLogs($$task: String!) {
          subscribeTaskLogs (
            task: $$task,
            from: 1
          ) {
            sequence
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject("task" -> JsString(task))))
  }
}
