package com.vyulabs.update.distribution.graphql.state

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.{FaultId, ServiceId}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import com.vyulabs.update.distribution.mongo.Sequenced
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class FaultReportInfoTest extends TestEnvironment {
  behavior of "Fault Report Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val faultsInfoCollection = collections.State_FaultReportsInfo
  val sequencesCollection = result(collections.Sequences)

  override def dbName = super.dbName + "-distribution"

  it should "add/get fault report info" in {
    addFaultReportInfo("fault1", "service1", 1, new Date())
    addFaultReportInfo("fault2", "service2", 2, new Date())

    assertResult((OK,
      ("""{"data":{"faultReports":[{"distribution":"distribution","report":{"faultId":"fault2","info":{"service":"service2","instance":"instance1"},"files":["core","log/service.log"]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition,
        adminContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReports (service: $$service) {
              distribution
              report {
                faultId
                info {
                  service
                  instance
                }
                files
              }
            }
          }
        """, None, variables = JsObject("service" -> JsString("service2")))))

    clear()
  }

  it should "clear old fault reports when max reports exceed" in {
    val date1 = new Date()
    addFaultReportInfo("fault1", "service1", 1, date1)
    val date2 = new Date()
    addFaultReportInfo("fault2", "service1", 2, date2)
    val date3 = new Date()
    addFaultReportInfo("fault3", "service1", 3, date3)
    val date4 = new Date()
    addFaultReportInfo("fault4", "service1", 4, date4)

    checkReportNotExists("fault1")
    checkReportExists("fault2", "service1", 2, date2)

    clear()
  }

  it should "clear old fault reports when expiration time came" in {
    val date1 = new Date()
    addFaultReportInfo("fault1", "service1", 1, date1)

    Thread.sleep(config.faultReports.expirationTimeout.toMillis)

    val date2 = new Date()
    addFaultReportInfo("fault2", "service1", 2, date2)
    checkReportNotExists("fault1")

    clear()
  }

  def addFaultReportInfo(faultId: FaultId, service: ServiceId, sequence: Long, date: Date): Unit = {
    assertResult((OK,
      ("""{"data":{"addFaultReportInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, distributionContext, graphql"""
        mutation FaultReportInfo($$date: Date!, $$faultId: String!, $$service: String!) {
          addFaultReportInfo (
            fault: {
              faultId: $$faultId,
              info: {
                date: $$date,
                instance: "instance1",
                serviceDirectory: "directory1",
                service: $$service,
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
      """, variables = JsObject("date" -> date.toJson, "faultId" -> JsString(faultId), "service" -> JsString(service)))))
    assert(distributionDir.getFaultReportFile(faultId).createNewFile())

    checkReportExists(faultId, service, sequence, date)
  }

  def checkReportExists(faultId: FaultId, service: ServiceId, sequence: Long, date: Date): Unit = {
    assertResult(Seq(
      Sequenced(sequence, DistributionFaultReport("distribution",
        ServiceFaultReport(faultId, FaultInfo(date, "instance1", service, "directory1", "Common", ServiceState(date, None, None, None, None, None, None, None), Seq("line1", "line2")), Seq("core", "log/service.log")))))
    )(result(faultsInfoCollection.findSequenced(Filters.eq("report.faultId", faultId))))
  }

  def checkReportNotExists(faultId: FaultId): Unit = {
    assertResult(Seq.empty)(result(faultsInfoCollection.find(Filters.eq("report.faultId", faultId))))
    assert(!distributionDir.getFaultReportFile(faultId).exists())
  }

  def clear(): Unit = {
    distributionDir.getFaultsDir().listFiles().foreach(_.delete())
    result(faultsInfoCollection.drop())
    result(sequencesCollection.delete())
  }
}
