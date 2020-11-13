package com.vyulabs.update.distribution.graphql.service

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientServiceLogLine, LogLine, ServiceLogLine}
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.ServiceLogLineDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class AddServiceLogsTest extends TestEnvironment {
  behavior of "State Info Requests"

  val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user1", UserRole.Client))

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
      ServiceLogLineDocument(1, new ClientServiceLogLine("own", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line1")))),
      ServiceLogLineDocument(2, new ClientServiceLogLine("own", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line2")))),
      ServiceLogLineDocument(3, new ClientServiceLogLine("own", new ServiceLogLine("service1", "instance1", "dir", LogLine(date, "line3")))))
    )(result(logsCollection.find()))
  }
}
