package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.Common._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceState}
import distribution.users.{UserInfo, UserRole}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.FaultReportDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import scala.concurrent.ExecutionContext

class GetFaultReportsTest extends TestEnvironment {
  behavior of "Fault Report Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val collection = result(collections.State_FaultReports)

  val graphqlContext = new GraphqlContext("distribution", versionHistoryConfig, collections, distributionDir, UserInfo("user", UserRole.Administrator))

  val distribution1 = "distribution1"
  val client2 = "client2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  override def beforeAll() = {
    result(collection.insert(
      FaultReportDocument(0, DistributionFaultReport("fault1", distribution1,
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
    result(collection.insert(
      FaultReportDocument(1, DistributionFaultReport("fault2", client2,
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core1")))))
    result(collection.insert(
      FaultReportDocument(2, DistributionFaultReport("fault3", distribution1,
        FaultInfo(new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
  }

  it should "get last fault reports for specified client" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"faultId":"fault3","distributionName":"distribution1","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (distribution: "distribution1", last: 1) {
            faultId
            distributionName
            info {
              serviceName
              instanceId
            }
            files
          }
        }
      """))
    )
  }

  it should "get last fault reports for specified service" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"faultId":"fault2","distributionName":"client2","info":{"serviceName":"serviceA","instanceId":"instance1"},"files":["fault.info","core1"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (service: "serviceA", last: 1) {
            faultId
            distributionName
            info {
              serviceName
              instanceId
            }
            files
          }
        }
      """))
    )
  }

  it should "get fault reports for specified service in parameters" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"faultId":"fault3","distributionName":"distribution1","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition,
        graphqlContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReports (service: $$service) {
            faultId
            distributionName
            info {
              serviceName
              instanceId
            }
            files
            }
          }
        """, None, variables = JsObject("service" -> JsString("serviceB")))))
  }
}
