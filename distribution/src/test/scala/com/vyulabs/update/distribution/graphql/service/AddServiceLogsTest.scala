package com.vyulabs.update.distribution.graphql.service

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionServiceLogLine, LogLine, ServiceLogLine}
import distribution.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.ServiceLogLineDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class AddServiceLogsTest extends TestEnvironment {
  behavior of "State Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(distributionName, versionHistoryConfig, collections, distributionDir, UserInfo("user1", UserRole.Distribution))

  val logsCollection = result(collections.State_ServiceLogs)

  it should "add service logs" in {
    val date = new Date()

    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          addServiceLogs (
            service: "service1",
            instance: "instance1",
            directory: "dir",
            logs: [
              { date: $$date, line: "line1" }
              { date: $$date, line: "line2" }
              { date: $$date, line: "line3" }
            ]
          )
        }
      """, variables = JsObject("date" -> date.toJson))))

    assertResult(Seq(
      ServiceLogLineDocument(1, new DistributionServiceLogLine("test", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line1")))),
      ServiceLogLineDocument(2, new DistributionServiceLogLine("test", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line2")))),
      ServiceLogLineDocument(3, new DistributionServiceLogLine("test", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line3")))))
    )(result(logsCollection.find()))
  }
}
