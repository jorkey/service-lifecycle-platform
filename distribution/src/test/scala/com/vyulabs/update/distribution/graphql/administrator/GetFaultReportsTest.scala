package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class GetFaultReportsTest extends TestEnvironment {
  behavior of "Fault Report Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val graphqlContext = GraphqlContext(Some(AccessToken("admin", Seq(UserRole.Administrator))), workspace)

  val distribution1 = "distribution1"
  val distribution2 = "distribution2"

  val instance1 = "instance1"
  val instance2 = "instance2"

  override def beforeAll() = {
    result(collections.State_FaultReportsInfo.insert(
      DistributionFaultReport(distribution1, ServiceFaultReport("fault1",
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
    result(collections.State_FaultReportsInfo.insert(
      DistributionFaultReport(distribution2, ServiceFaultReport("fault2",
        FaultInfo(new Date(), instance1, "directory", "serviceA", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core1")))))
    result(collections.State_FaultReportsInfo.insert(
      DistributionFaultReport(distribution1, ServiceFaultReport("fault3",
        FaultInfo(new Date(), instance2, "directory", "serviceB", CommonServiceProfile, ServiceState(new Date(), None, None, None, None, None, None, None), Seq.empty),
        Seq("fault.info", "core")))))
  }

  it should "get last fault reports for specified client" in {
    assertResult((OK,
      ("""{"data":{"faultReportsInfo":[{"distributionName":"distribution1","report":{"faultId":"fault3","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReportsInfo (distribution: "distribution1", last: 1) {
            distributionName
            report {
              faultId
              info {
                serviceName
                instanceId
              }
              files
            }
          }
        }
      """))
    )
  }

  it should "get last fault reports for specified service" in {
    assertResult((OK,
      ("""{"data":{"faultReportsInfo":[{"distributionName":"distribution2","report":{"faultId":"fault2","info":{"serviceName":"serviceA","instanceId":"instance1"},"files":["fault.info","core1"]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition, graphqlContext, graphql"""
        query {
          faultReportsInfo (service: "serviceA", last: 1) {
            distributionName
            report {
              faultId
              info {
                serviceName
                instanceId
              }
              files
            }
          }
        }
      """))
    )
  }

  it should "get fault reports for specified service in parameters" in {
    assertResult((OK,
      ("""{"data":{"faultReportsInfo":[{"distributionName":"distribution1","report":{"faultId":"fault3","info":{"serviceName":"serviceB","instanceId":"instance2"},"files":["fault.info","core"]}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ClientSchemaDefinition,
        graphqlContext, graphql"""
          query FaultsQuery($$service: String!) {
            faultReportsInfo (service: $$service) {
              distributionName
              report {
                faultId
                info {
                  serviceName
                  instanceId
                }
                files
              }
            }
          }
        """, None, variables = JsObject("service" -> JsString("serviceB")))))
  }
}
