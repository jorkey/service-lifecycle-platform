package com.vyulabs.update.distribution.graphql.updater

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ActorMaterializer, Materializer}
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import com.vyulabs.update.distribution.mongo.Sequenced
import sangria.macros.LiteralGraphQLStringContext
import spray.json._
import com.vyulabs.update.common.utils.JsonFormats._

import java.util.Date
import scala.concurrent.ExecutionContext

class AddServiceLogsTest extends TestEnvironment {
  behavior of "Service Logs Requests"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  override def dbName = super.dbName + "-service"

  val graphqlContext = GraphqlContext(Some(AccessToken("service1", Seq(UserRole.Updater))), workspace)

  val logsCollection = collections.State_ServiceLogs

  it should "add service logs" in {
    val date = new Date()

    assertResult((OK,
      ("""{"data":{"addServiceLogs":true}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.UpdaterSchemaDefinition, graphqlContext, graphql"""
        mutation ServicesState($$date: Date!) {
          addServiceLogs (
            service: "service1",
            instance: "instance1",
            process: "process1",
            directory: "dir",
            logs: [
              { date: $$date, level: "INFO", unit: "none", message: "line1" }
              { date: $$date, level: "DEBUG", unit: "none", message: "line2" }
              { date: $$date, level: "ERROR", unit: "none", message: "line3" }
            ]
          )
        }
      """, variables = JsObject("date" -> date.toJson))))

    assertResult(Seq(
      Sequenced(1, new ServiceLogLine("test",
        "service1", None, "instance1", "process1", "dir", LogLine(date, "INFO", "none", "line1", None))),
      Sequenced(2, new ServiceLogLine("test",
        "service1", None, "instance1", "process1", "dir", LogLine(date, "DEBUG", "none", "line2", None))),
      Sequenced(3, new ServiceLogLine("test",
        "service1", None, "instance1", "process1", "dir", LogLine(date, "ERROR", "none", "line3", None))))
    )(result(logsCollection.findSequenced()))
  }
}
