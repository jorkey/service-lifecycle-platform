package com.vyulabs.update.distribution.logger

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.qos.logback.classic.Level
import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl}
import com.vyulabs.update.common.logger.{LogBuffer, LogUploader, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.GraphqlSchema
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.net.URL

class LogUploaderTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Log trace uploader"

  implicit val timer = new ThreadTimer()

  val logger = Utils.getLogbackLogger(this.getClass)
  logger.setLevel(Level.INFO)
  val appender = new TraceAppender()
  logger.addAppender(appender)

  val route = distribution.route
  var server = Http().newServerAt("0.0.0.0", 8084).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val httpClient = new HttpClientImpl(new URL("http://updater:updater@localhost:8084"))
  val distributionClient = new DistributionClient(httpClient)

  val sender = new LogUploader("service1", None, "instance1", distributionClient)
  val buffer = new LogBuffer("Test", "PROCESS", sender, 3, 6)

  appender.addListener(buffer)
  appender.start()

  it should "send log records to distribution server" in {
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")

    Thread.sleep(5000)

    assertResult((OK,
      ("""{"data":{"serviceLogs":[""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"INFO","message":"`Test` started"}},""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"INFO","message":"log line 1"}},""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"WARN","message":"log line 2"}},""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"ERROR","message":"log line 3"}},""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"WARN","message":"log line 4"}},""" +
        """{"distribution":"test","service":"service1","instance":"instance1","line":{"level":"INFO","message":"log line 5"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.SchemaDefinition, adminContext, graphql"""
        query ServiceLogs($$distribution: String!, $$service: String!, $$instance: String!, $$process: String!, $$directory: String!) {
          serviceLogs (distribution: $$distribution, service: $$service, instance: $$instance, process: $$process, directory: $$directory) {
            distribution
            service
            instance
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
        "process" -> JsString(ProcessHandle.current.pid.toString),
        "directory" -> JsString(new java.io.File(".").getCanonicalPath()))))
    )
  }
}
