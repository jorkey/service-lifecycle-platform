package com.vyulabs.update.distribution.graphql.service

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, DistributionServiceLogLine, FaultInfo, LogLine, ServiceFaultReport, ServiceLogLine, ServiceState}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.{FaultReportDocument, ServiceLogLineDocument}
import distribution.users.{UserInfo, UserRole}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class AddFaultReportInfoTest extends TestEnvironment {
  behavior of "Fault Report Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(distributionName, versionHistoryConfig, collections, distributionDir, UserInfo("user1", UserRole.Distribution))

  val faultsInfoCollection = result(collections.State_FaultReportsInfo)

  it should "add fault report info" in {
    val date = new Date()

    assertResult((OK,
      ("""{"data":{"addServiceFaultReportInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation FaultReportInfo($$date: Date!) {
          addServiceFaultReportInfo (
            fault: {
              faultId: "fault1",
              info: {
                date: $$date,
                instanceId: "instance1",
                serviceDirectory: "directory1",
                serviceName: "service1",
                serviceProfile: "Common",
                state: {
                  date: $$date
                },
                logTail: [
                   "line1",
                   "line2"
                ]
              },
              files: [
                "core",
                "log/service.log"
              ]
            }
          )
        }
      """, variables = JsObject("date" -> date.toJson))))

    assertResult(Seq(
      FaultReportDocument(1, DistributionFaultReport(distributionName,
        ServiceFaultReport("fault1", FaultInfo(date, "instance1", "directory1", "service1", "Common", ServiceState(date, None, None, None, None, None, None, None),
          Seq("line1", "line2")), Seq("core", "log/service.log")))))
    )(result(faultsInfoCollection.find()))
  }
}
