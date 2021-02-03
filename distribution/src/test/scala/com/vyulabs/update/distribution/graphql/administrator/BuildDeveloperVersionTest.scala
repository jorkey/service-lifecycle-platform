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
import com.vyulabs.update.common.info.{UserInfo, UserRole}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json.{JsObject, JsString}

import scala.concurrent.ExecutionContext

class BuildDeveloperVersionTest extends TestEnvironment {
  behavior of "Build Developer Version"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = GraphqlContext(UserInfo("admin", UserRole.Administrator), workspace)

  it should "build developer version" in {
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

    println(logInput.requestNext())
  }

  def subscribeTaskLogs(taskId: TaskId): ToResponseMarshallable = {
    result(graphql.executeSubscriptionQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        subscription SubscribeTaskLogs($$taskId: String!) {
          subscribeTaskLogs (
            task: $$taskId
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
