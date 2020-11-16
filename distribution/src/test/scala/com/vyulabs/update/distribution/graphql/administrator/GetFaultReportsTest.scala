package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.common.Common._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.mongo.FaultReportDocument
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class GetFaultReportsTest extends TestEnvironment {
  behavior of "AdaptationMeasure"

  val collection = result(collections.State_FaultReports)

  val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user", UserRole.Administrator))

  val client1 = "client1"
  val client2 = "client2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  override def beforeAll() = {
    result(collection.insert(
      FaultReportDocument(0, ClientFaultReport("fault1", client1,
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
    result(collection.insert(
      FaultReportDocument(1, ClientFaultReport("fault2", client2,
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core1")))))
    result(collection.insert(
      FaultReportDocument(2, ClientFaultReport("fault3", client1,
        FaultInfo(new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
  }

  it should "get last fault reports for specified client" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"faultId":"fault3","clientName":"client1","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (client: "client1", last: 1) {
            faultId
            clientName
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
      ("""{"data":{"faultReports":[{"faultId":"fault2","clientName":"client2","info":{"serviceName":"serviceA","instanceId":"instance1"},"files":["fault.info","core1"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (service: "serviceA", last: 1) {
            faultId
            clientName
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
      ("""{"data":{"faultReports":[{"faultId":"fault3","clientName":"client1","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition,
        graphqlContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReports (service: $$service) {
            faultId
            clientName
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
