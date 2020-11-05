package com.vyulabs.update.distribution.graphql.administrator

import java.util.Date

import akka.http.scaladsl.model.StatusCodes.OK
import com.vyulabs.update.common.Common._
import com.vyulabs.update.distribution.{GraphqlTestEnvironment}
import com.vyulabs.update.info.{ClientFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class FaultReportsTest extends GraphqlTestEnvironment {
  behavior of "AdaptationMeasure"

  val collection = result(collections.State_FaultReports)

  val graphqlContext = new GraphqlContext(versionHistoryConfig, distributionDir, collections, UserInfo("user", UserRole.Administrator))

  val client1 = "client1"
  val client2 = "client2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  override def beforeAll() = {
    result(collection.insert(
      ClientFaultReport(client1, "fault1", Seq("fault.info", "core"),
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
    result(collection.insert(
      ClientFaultReport(client2, "fault1", Seq("fault.info", "core1"),
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
    result(collection.insert(
      ClientFaultReport(client1, "fault2", Seq("fault.info", "core"),
        FaultInfo(new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty))))
  }

  it should "get last fault reports for specified client" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"clientName":"client1","reportDirectory":"fault2","reportFiles":["fault.info","core"],"faultInfo":{"serviceName":"serviceB","instanceId":"instance2"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (client: "client1", last: 1) {
            clientName
            reportDirectory
            reportFiles
            faultInfo {
              serviceName
              instanceId
            }
          }
        }
      """))
    )
  }

  it should "get last fault reports for specified service" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"clientName":"client2","reportDirectory":"fault1","reportFiles":["fault.info","core1"],"faultInfo":{"serviceName":"serviceA","instanceId":"instance1"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReports (service: "serviceA", last: 1) {
            clientName
            reportDirectory
            reportFiles
            faultInfo {
              serviceName
              instanceId
            }
          }
        }
      """))
    )
  }

  it should "get fault reports for specified service in parameters" in {
    assertResult((OK,
      ("""{"data":{"faultReports":[{"clientName":"client1","reportDirectory":"fault2","reportFiles":["fault.info","core"],"faultInfo":{"serviceName":"serviceB","instanceId":"instance2"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition,
        graphqlContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReports (service: $$service) {
              clientName
              reportDirectory
              reportFiles
              faultInfo {
                serviceName
                instanceId
              }
            }
          }
        """, None, variables = JsObject("service" -> JsString("serviceB")))))
  }
}
