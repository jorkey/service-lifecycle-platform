package com.vyulabs.update.distribution.graphql.administrator

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.config.BuilderConfig
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json.{JsObject, JsString, _}

import java.io.File
import scala.concurrent.ExecutionContext

class BuildClientVersionTest extends TestEnvironment {
  behavior of "Build Client Version"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => {
    ex.printStackTrace(); log.error("Uncatched exception", ex)
  })

  val graphqlContext = GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  val dummyBuilder = new File(builderDirectory, "builder.sh")
  IoUtils.writeBytesToFile(dummyBuilder, "echo \"Builder started\"\nsleep 1\necho \"Builder continued\"\nsleep 1\necho \"Builder finished\"".getBytes)

  override def builderConfig = BuilderConfig(Some(builderDirectory.toString), None)

  it should "build client version" in {
    val buildResponse = result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          buildClientVersion (service: "service1", developerVersion: "test-1.1.1", clientVersion: "test-1.1.1")
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val taskId = data.fields.get("buildClientVersion").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(taskId)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    val builderDir = config.builderConfig.builderDirectory.get
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":2,"logLine":{"line":{"level":"INFO","message":"Logger `Build client version test-1.1.1 of service service1` started"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":3,"logLine":{"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(builder.sh, buildClientVersion, distributionName=test, service=service1, developerVersion=test-1.1.1, clientVersion=test-1.1.1, author=admin) in directory ${builderDir}"}}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":6,"logLine":{"line":{"level":"INFO","message":"Builder started"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":7,"logLine":{"line":{"level":"INFO","message":"Builder continued"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":8,"logLine":{"line":{"level":"INFO","message":"Builder finished"}}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":10,"logLine":{"line":{"level":"INFO","message":"Logger `Build client version test-1.1.1 of service service1` finished successfully"}}}}}"""))
    logInput.expectComplete()
  }

  it should "cancel of building developer version" in {
    val buildResponse = result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation {
          buildDeveloperVersion (service: "service1", version: "1.1.1", branches: ["master", "master"], comment: "Test version")
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val taskId = data.fields.get("buildDeveloperVersion").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(taskId)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    val builderDir = config.builderConfig.builderDirectory.get
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":2,"logLine":{"line":{"level":"INFO","message":"Logger `Build developer version 1.1.1 of service service1` started"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":3,"logLine":{"line":{"level":"INFO","message":"Start command /bin/sh with arguments List(builder.sh, buildDeveloperVersion, distributionName=test, service=service1, version=1.1.1, author=admin, sourceBranches=master,master}, comment=Test version) in directory ${builderDir}"}}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":6,"logLine":{"line":{"level":"INFO","message":"Builder started"}}}}}"""))

    assertResult((OK, ("""{"data":{"cancelTask":true}}""").parseJson))(result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        mutation CancelTask($$taskId: String!) {
          cancelTask (task: $$taskId)
        }
      """, variables = JsObject("taskId" -> JsString(taskId)))))

    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    println(logInput.requestNext())
    logInput.expectComplete()
  }

  it should "run builder for remote distribution" in {
    val buildResponse = result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation {
          runBuilder (arguments: ["buildDeveloperVersion", "distributionName=test", "service=service1", "version=1.1.1", "author=admin", "sourceBranches=master,master"])
        }
      """))
    assertResult(OK)(buildResponse._1)
    val fields = buildResponse._2.asJsObject.fields
    val data = fields.get("data").get.asJsObject
    val taskId = data.fields.get("runBuilder").get.toString().drop(1).dropRight(1)

    val subscribeResponse = subscribeTaskLogs(taskId)
    val logSource = subscribeResponse.value.asInstanceOf[Source[ServerSentEvent, NotUsed]]
    val logInput = logSource.runWith(TestSink.probe[ServerSentEvent])

    val builderDir = config.builderConfig.builderDirectory.get
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":2,"logLine":{"line":{"level":"INFO","message":"Logger `Run local builder by remote distribution` started"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":3,"logLine":{"line":{"level":"INFO","message":"Start command /bin/sh with arguments Vector(builder.sh, buildDeveloperVersion, distributionName=test, service=service1, version=1.1.1, author=admin, sourceBranches=master,master) in directory ${builderDir}"}}}}}"""))
    logInput.requestNext()
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":6,"logLine":{"line":{"level":"INFO","message":"Builder started"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":7,"logLine":{"line":{"level":"INFO","message":"Builder continued"}}}}}"""))
    logInput.requestNext(
      ServerSentEvent(s"""{"data":{"subscribeTaskLogs":{"sequence":8,"logLine":{"line":{"level":"INFO","message":"Builder finished"}}}}}"""))
    logInput.requestNext()
    logInput.requestNext(
      ServerSentEvent("""{"data":{"subscribeTaskLogs":{"sequence":10,"logLine":{"line":{"level":"INFO","message":"Logger `Run local builder by remote distribution` finished successfully"}}}}}"""))
    logInput.expectComplete()
  }

  def subscribeTaskLogs(taskId: TaskId): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        subscription SubscribeTaskLogs($$taskId: String!) {
          subscribeTaskLogs (
            task: $$taskId,
            from: 1
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
      """, variables = JsObject("taskId" -> JsString(taskId))))
  }
}
