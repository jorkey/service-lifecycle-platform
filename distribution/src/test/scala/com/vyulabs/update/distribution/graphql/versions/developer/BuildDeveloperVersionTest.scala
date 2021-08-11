package com.vyulabs.update.distribution.graphql.versions.developer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json.{JsObject, JsString, _}

import java.io.File
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class BuildDeveloperVersionTest extends TestEnvironment {
  behavior of "Build Developer Version"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  assert(IoUtils.writeBytesToFile(new File(builderDirectory, "builder.sh"),
    ("echo \"2021-03-23 19:12:19.146 INFO Unit1 Builder started\"\n" +
    "sleep 1\n" +
    "echo \"2021-03-23 19:12:20.150 INFO Unit2 Builder continued\"\n" +
    "sleep 1\n" +
    "echo \"2021-03-23 19:12:21.100 INFO Unit3 Builder finished\"").getBytes))

  assert(IoUtils.writeBytesToFile(new File(consumerBuilderDirectory, "builder.sh"),
    ("echo \"2021-03-23 19:12:19.146 INFO Unit1 Builder started\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:20.150 INFO Unit2 Builder continued\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:21.100 INFO Unit3 Builder finished\"").getBytes))

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
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":3,"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=developer, sources=[], comment=Test version) in directory ${builderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":6,"line":{"level":"INFO","message":"Builder started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":7,"line":{"level":"INFO","message":"Builder continued"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":8,"line":{"level":"INFO","message":"Builder finished"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":10,"line":{"level":"","message":"Builder process terminated with status 0"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":11,"line":{"level":"INFO","message":"Set developer desired versions List(DeveloperDesiredVersionDelta(service1,Some(test-1.1.1)))"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":12,"line":{"level":"INFO","message":"`Build developer version 1.1.1 of service service1` finished successfully"}}}}"""))

    expectComplete(logInput)
  }

  it should "cancel of building developer version" in {
    setSequence("state.serviceLogs", 100)

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
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":101,"line":{"level":"INFO","message":"`Build developer version 1.1.1 of service service1` started"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":103,"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=developer, sources=[], comment=Test version) in directory ${builderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":106,"line":{"level":"INFO","message":"Builder started"}}}}"""))

    assertResult((OK, ("""{"data":{"cancelTask":true}}""").parseJson))(result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext, graphql"""
        mutation CancelTask($$task: String!) {
          cancelTask (task: $$task)
        }
      """, variables = JsObject("task" -> JsString(task)))))

    expectComplete(logInput)
  }

  it should "run builder" in {
    setSequence("state.serviceLogs", 200)

    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, consumerContext, graphql"""
        mutation {
          runBuilder (accessToken: "qwe", arguments: ["buildDeveloperVersion", "distribution=test", "service=service1", "version=1.1.1", "author=admin", "sources=[]"])
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
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":201,"line":{"level":"INFO","message":"`Run builder by remote distribution` started"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":203,"line":{"level":"INFO","message":"Start command /bin/sh with arguments Vector(./builder.sh, buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=admin, sources=[]) in directory ${consumerBuilderDirectory}"}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":206,"line":{"level":"INFO","message":"Builder started"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":207,"line":{"level":"INFO","message":"Builder continued"}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":208,"line":{"level":"INFO","message":"Builder finished"}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":210,"line":{"level":"","message":"Builder process terminated with status 0"}}}}"""))
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":211,"line":{"level":"INFO","message":"`Run builder by remote distribution` finished successfully"}}}}"""))

    expectComplete(logInput)
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

  @tailrec
  private def expectComplete(input: TestSubscriber.Probe[ServerSentEvent]): Unit = {
    input.request(1)
    val r = input.expectNextOrComplete()
    if (!r.isLeft) {
      expectComplete(input)
    }
  }
}
