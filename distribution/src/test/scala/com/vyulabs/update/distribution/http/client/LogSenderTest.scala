package com.vyulabs.update.distribution.http.client

import akka.http.scaladsl.Http
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.qos.logback.classic.{Level, Logger}
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.graphql.{DistributionClient, HttpClientImpl, LogSender}
import com.vyulabs.update.logger.{LogBuffer, TraceAppender}
import org.slf4j.LoggerFactory

import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class LogSenderTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Log trace sender"

  val logger: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
  logger.setLevel(Level.INFO)
  val appender = new TraceAppender()
  appender.start()
  logger.addAppender(appender)

  val route = distribution.route
  var server = Http().newServerAt("0.0.0.0", 8084).adaptSettings(s => s.withTransparentHeadRequests(true))
  server.bind(route)

  val httpClient = new HttpClientImpl(new URL("http://admin:admin@localhost:8084"))
  val distributionClient = new DistributionClient(distributionName, httpClient)

  val sender = new LogSender("service1", "instance1", distributionClient)
  val buffer = new LogBuffer(sender, 3, 6)

  appender.addListener(buffer)

  system.scheduler.scheduleWithFixedDelay(FiniteDuration(1, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.SECONDS))(new Runnable {
    override def run(): Unit = { buffer.flush() }
  })

  it should "send log records to distribution server" in {
    logger.info("log line 1")
    logger.warn("log line 2")
    logger.error("log line 3")
    logger.warn("log line 4")
    logger.info("log line 5")

    Thread.sleep(3000)
    println(result(result(collections.State_ServiceLogs).find()))
  }
}
