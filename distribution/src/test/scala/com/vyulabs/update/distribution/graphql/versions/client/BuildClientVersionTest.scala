package com.vyulabs.update.distribution.graphql.versions.client

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
import com.vyulabs.update.common.config.BuildServiceConfig
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json.{JsObject, JsString, _}

import java.io.File
import scala.annotation.tailrec
import scala.concurrent.ExecutionContext

class BuildClientVersionTest extends TestEnvironment {
  behavior of "Build Client Version"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, Utils.logException(log, "Uncatched exception", _))

  result(for {
    _ <- collections.Developer_BuildServices.insert(BuildServiceConfig("service1", None, Seq.empty, Seq.empty, Seq.empty, Seq.empty))
  } yield {})

  IoUtils.writeBytesToFile(new File(builderDirectory, "builder.sh"),
    ("echo \"2021-03-23 19:12:19.146 INFO Unit1 Builder started\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:20.150 INFO Unit2 Builder continued\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:21.100 INFO Unit3 Builder finished\"").getBytes)

  IoUtils.writeBytesToFile(new File(consumerBuilderDirectory, "builder.sh"),
    ("echo \"2021-03-23 19:12:19.146 INFO Unit1 Builder started\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:20.150 INFO Unit2 Builder continued\"\n" +
      "sleep 1\n" +
      "echo \"2021-03-23 19:12:21.100 INFO Unit3 Builder finished\"").getBytes)

  it should "build client version" in {
    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation {
          buildClientVersions (versions: [{ service: "service1", version: { distribution: "test", build: [1,1,1] } }])
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("buildClientVersions").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    expectComplete(logInput)
  }

  it should "cancel of building developer version" in {
    collections.Log_Lines.setSequence(100)

    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, developerContext, graphql"""
        mutation {
          buildDeveloperVersion (service: "service1", version: { build: [1,1,1] }, comment: "Test version")
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("buildDeveloperVersion").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    expectMessage(logInput, "Builder started")

    assertResult((OK, ("""{"data":{"cancelTask":true}}""").parseJson))(result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        mutation CancelTask($$task: String!) {
          cancelTask (task: $$task)
        }
      """, variables = JsObject("task" -> JsString(task)))))

    expectComplete(logInput)
  }

  it should "run builder" in {
    collections.Log_Lines.setSequence(200)

    val buildResponse = result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, consumerContext, graphql"""
        mutation {
          runBuilder (accessToken: "qwe", arguments: ["buildDeveloperVersion", "distribution=test", "service=service1", "version=1.1.1", "author=admin"], service: "service1", environment: [])
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val task = data.fields.get("runBuilder").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(task)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    expectMessage(logInput, "Finished successfully, task RunBuilderByRemoteDistribution with parameters: distribution=consumer, accessToken=qwe, environment=, arguments=buildDeveloperVersion, distribution=test, service=service1, version=1.1.1, author=admin")

    expectComplete(logInput)
  }

  def subscribeTaskLogs(task: TaskId): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQueryToSSE(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        subscription SubscribeTaskLogs($$task: String!) {
          subscribeLogs (
            task: $$task,
            from: 1
          ) {
            level
            message
          }
        }
      """, variables = JsObject("task" -> JsString(task))))
  }

  @tailrec
  private def expectMessage(input: TestSubscriber.Probe[ServerSentEvent], message: String): Unit = {
    val e = input.requestNext()
    val json = e.data.parseJson
    val messages = json.asJsObject.fields.get("data").get
      .asJsObject.fields.get("subscribeLogs").get.asInstanceOf[JsArray]
        .elements.map(_.asJsObject.fields.get("message").get.asInstanceOf[JsString].value)
    println(messages)
    if (!messages.contains(message)) {
      expectMessage(input, message)
    }
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
