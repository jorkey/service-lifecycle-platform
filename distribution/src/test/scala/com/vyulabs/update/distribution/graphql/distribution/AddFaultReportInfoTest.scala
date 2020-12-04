package com.vyulabs.update.distribution.graphql.distribution

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.FaultId
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceFaultReport, ServiceState, UserInfo, UserRole}
import com.vyulabs.update.utils.Utils.DateJson._
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.FaultReportDocument
import org.mongodb.scala.bson.BsonDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class AddFaultReportInfoTest extends TestEnvironment {
  behavior of "Fault Report Info Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = new GraphqlContext(UserInfo("distribution1", UserRole.Distribution), workspace)

  val faultsInfoCollection = result(collections.State_FaultReportsInfo)
  val sequencesCollection = result(collections.Sequences)

  override def dbName = super.dbName + "-distribution"

  it should "add fault report info" in {
    addFaultReportInfo("fault1", 1, new Date())

    clear()
  }

  it should "clear old fault reports when max reports exceed" in {
    val date1 = new Date()
    addFaultReportInfo("fault1", 1, date1)
    val date2 = new Date()
    addFaultReportInfo("fault2", 2, date2)
    val date3 = new Date()
    addFaultReportInfo("fault3", 3, date3)
    val date4 = new Date()
    addFaultReportInfo("fault4", 4, date4)

    checkReportNotExists("fault1")
    checkReportExists("fault2", 2, date2)

    clear()
  }

  it should "clear old fault reports when expiration time came" in {
    val date1 = new Date()
    addFaultReportInfo("fault1", 1, date1)

    Thread.sleep(faultReportsConfig.expirationPeriodMs)

    val date2 = new Date()
    addFaultReportInfo("fault2", 2, date2)
    checkReportNotExists("fault1")

    clear()
  }

  def addFaultReportInfo(faultId: FaultId, sequence: Long, date: Date): Unit = {
    assertResult((OK,
      ("""{"data":{"addFaultReportInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.DistributionSchemaDefinition, graphqlContext, graphql"""
        mutation FaultReportInfo($$date: Date!, $$faultId: String!) {
          addFaultReportInfo (
            fault: {
              faultId: $$faultId,
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
      """, variables = JsObject("date" -> date.toJson, "faultId" -> JsString(faultId)))))
    assert(distributionDir.getFaultReportFile(faultId).createNewFile())

    checkReportExists(faultId, sequence, date)
  }

  def checkReportExists(faultId: FaultId, sequence: Long, date: Date): Unit = {
    assertResult(Seq(
      FaultReportDocument(sequence, DistributionFaultReport("distribution1",
        ServiceFaultReport(faultId, FaultInfo(date, "instance1", "directory1", "service1", "Common", ServiceState(date, None, None, None, None, None, None, None),
          Seq("line1", "line2")), Seq("core", "log/service.log")))))
    )(result(faultsInfoCollection.find(Filters.eq("fault.report.faultId", faultId))))
  }

  def checkReportNotExists(faultId: FaultId): Unit = {
    assertResult(Seq.empty)(result(faultsInfoCollection.find(Filters.eq("fault.report.faultId", faultId))))
    assert(!distributionDir.getFaultReportFile(faultId).exists())
  }

  def clear(): Unit = {
    result(faultsInfoCollection.delete(new BsonDocument()))
    distributionDir.getFaultsDir().listFiles().foreach(_.delete())
    result(sequencesCollection.delete(new BsonDocument()))
  }
}
