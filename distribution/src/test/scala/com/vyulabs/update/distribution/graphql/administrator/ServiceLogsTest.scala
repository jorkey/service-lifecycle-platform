package com.vyulabs.update.distribution.graphql.administrator

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.Utils.DateJson._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.util.Date
import scala.concurrent.ExecutionContext

class ServiceLogsTest extends TestEnvironment {
  behavior of "Service Logs Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def dbName = super.dbName + "-administrator"

  val graphqlContext = new GraphqlContext(UserInfo("administrator", UserRole.Administrator), workspace)

  val logsCollection = result(collections.State_ServiceLogs)

  it should "add/get service logs" in {
    val date = new Date()

    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.ServiceSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          addServiceLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            logs: [
              { date: $$date, level: "INFO", message: "line1" }
              { date: $$date, level: "DEBUG", message: "line2" }
              { date: $$date, level: "ERROR", message: "line3" }
            ]
          )
        }
      """, variables = JsObject("date" -> date.toJson))))

    assertResult((OK,
      ("""{"data":{"serviceLogs":[""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"INFO","message":"line1"},"serviceName":"service1","directory":"dir"},""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"DEBUG","message":"line2"},"serviceName":"service1","directory":"dir"},""" +
       """{"instanceId":"instance1","distributionName":"test","line":{"level":"ERROR","message":"line3"},"serviceName":"service1","directory":"dir"}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query ServiceLogs($$distribution: String!, $$service: String!, $$instance: String!, $$process: String!, $$directory: String!) {
          serviceLogs (distribution: $$distribution, service: $$service, instance: $$instance, process: $$process, directory: $$directory) {
            distributionName
            serviceName
            instanceId
            directory
            line {
              level
              message
            }
          }
        }
      """, variables = JsObject(
        "distribution" -> JsString("test"),
        "service" -> JsString("service1"),
        "instance" -> JsString("instance1"),
        "process" -> JsString("process1"),
        "directory" -> JsString("dir"))))
    )
  }
}
