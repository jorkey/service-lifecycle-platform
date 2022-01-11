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

  val faultsInfoCollection = collections.Faults_ReportsInfo
  val sequencesCollection = result(collections.Sequences)

  override def dbName = super.dbName + "-distribution"

  it should "add/get fault report info" in {
    addFaultReportInfo("fault1", "service1", 1, new Date())
    addFaultReportInfo("fault2", "service2", 2, new Date())

    assertResult((OK,
      ("""{"data":{"faults":[{"distribution":"consumer","payload":{"fault":"fault2","info":{"service":"service2","instance":"instance1"},"files":[{ "path": "core", "length": 123456789 }, { "path": "log/service.log", "length": 12345 }]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition,
        adminContext, graphql"""
          query FaultsQuery($$service: String!) {
            faults (service: $$service) {
              distribution
              payload {
                fault
                info {
                  service
                  instance
                }
                files {
                  path
                  length
                }
              }
            }
          }
        """, None, variables = JsObject("service" -> JsString("service2")))))

    clear()
  }

  it should "clear old fault reports when max reports exceed" in {
    val time1 = new Date()
    addFaultReportInfo("fault1", "service1", 1, time1)
    val time2 = new Date()
    addFaultReportInfo("fault2", "service1", 2, time2)
    val time3 = new Date()
    addFaultReportInfo("fault3", "service1", 3, time3)
    val time4 = new Date()
    addFaultReportInfo("fault4", "service1", 4, time4)

    checkReportNotExists("fault1")
    checkReportExists("fault2", "service1", 2, time2)

    clear()
  }

  it should "clear old fault reports when expiration time came" in {
    val time1 = new Date()
    addFaultReportInfo("fault1", "service1", 1, time1)

    Thread.sleep(config.faultReports.expirationTimeout.toMillis*2)

    val time2 = new Date()
    addFaultReportInfo("fault2", "service1", 2, time2)
    checkReportNotExists("fault1")

    clear()
  }

  def addFaultReportInfo(fault: FaultId, service: ServiceId, sequence: Long, time: Date): Unit = {
    assertResult((OK,
      ("""{"data":{"addFaultReportInfo":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, consumerContext, graphql"""
        mutation FaultReportInfo($$time: Date!, $$fault: String!, $$service: String!) {
          addFaultReportInfo (
            fault: {
              fault: $$fault,
              info: {
                time: $$time,
                instance: "instance1",
                serviceDirectory: "directory1",
                service: $$service,
                state: {
                  time: $$time
                },
                logTail: []
              },
              files: [
                { path: "core", length: 123456789 },
                { path: "log/service.log", length: 12345 }
              ]
            }
          )
        }
      """, variables = JsObject("time" -> time.toJson, "fault" -> JsString(fault), "service" -> JsString(service)))))
    assert(distributionDir.getFaultReportFile(fault).createNewFile())

    checkReportExists(fault, service, sequence, time)
  }

  def checkReportExists(id: FaultId, service: ServiceId, sequence: Long, time: Date): Unit = {
    assertResult(Seq(
      Sequenced(sequence, DistributionFaultReport("consumer",
        ServiceFaultReport(id,
          FaultInfo(time, "instance1", service, None, "directory1", ServiceState(time, None, None, None, None, None, None, None), Seq.empty),
          Seq(FileInfo("core", 123456789), FileInfo("log/service.log", 12345))))))
    )(result(faultsInfoCollection.findSequenced(Filters.eq("payload.fault", id))))
  }

  def checkReportNotExists(id: FaultId): Unit = {
    assertResult(Seq.empty)(result(faultsInfoCollection.find(Filters.eq("payload.fault", id))))
    assert(!distributionDir.getFaultReportFile(id).exists())
  }

  def clear(): Unit = {
    distributionDir.getFaultsDir().listFiles().foreach(_.delete())
    result(faultsInfoCollection.drop())
    result(sequencesCollection.delete())
  }
}
