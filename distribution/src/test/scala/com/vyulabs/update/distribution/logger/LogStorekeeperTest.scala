package com.vyulabs.update.distribution.logger

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.qos.logback.classic.Level
import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.logger.{LogBuffer, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{GraphqlContext, GraphqlSchema}
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

class LogStorekeeperTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Log trace writer"

  implicit val timer = new ThreadTimer()

  val logger = Utils.getLogbackLogger(this.getClass)
  logger.setLevel(Level.INFO)
  val appender = new TraceAppender()
  logger.addAppender(appender)

  val storekeeper = new LogStorekeeper("service1", None, "instance1", collections.Log_Lines)
  val buffer = new LogBuffer("Test", "PROCESS", storekeeper, 3, 6)

  appender.addListener(buffer)
  appender.start()

  it should "store log records" in {
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")

    Thread.sleep(5000)

    assertResult((OK,
      ("""{"data":{"logs":[""" +
        """{"level":"INFO","message":"Started Test"},""" +
        """{"level":"INFO","message":"log line 1"},""" +
        """{"level":"WARN","message":"log line 2"},""" +
        """{"level":"ERROR","message":"log line 3"},""" +
        """{"level":"WARN","message":"log line 4"},""" +
        """{"level":"INFO","message":"log line 5"}}]}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query ServiceLogs($$service: String!, $$instance: String!, $$process: String!, $$directory: String!) {
          logs (service: $$service, instance: $$instance, process: $$process, directory: $$directory) {
            level
            message
          }
        }
      """, variables = JsObject(
        "service" -> JsString("service1"),
        "instance" -> JsString("instance1"),
        "process" -> JsString(ProcessHandle.current.pid.toString),
        "directory" -> JsString(new java.io.File(".").getCanonicalPath()))))
    )
  }
}
