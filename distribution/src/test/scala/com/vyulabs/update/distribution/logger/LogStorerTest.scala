package com.vyulabs.update.distribution.logger

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.testkit.ScalatestRouteTest
import ch.qos.logback.classic.Level
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.{DistributionClient, HttpClientImpl}
import com.vyulabs.update.info.{UserInfo, UserRole}
import com.vyulabs.update.logger.{LogBuffer, LogSender, TraceAppender}
import com.vyulabs.update.utils.Utils
import distribution.graphql.{GraphqlContext, GraphqlSchema}
import distribution.logger.LogStorer
import sangria.macros.LiteralGraphQLStringContext
import spray.json._

import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class LogStorerTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Log trace writer"

  val logger = Utils.getLogbackLogger(this.getClass)
  logger.setLevel(Level.INFO)
  val appender = new TraceAppender()
  appender.start()
  logger.addAppender(appender)

  val sender = new LogStorer(distributionName, "service1", "instance1", collections)
  val buffer = new LogBuffer(sender, 3, 6)

  appender.addListener(buffer)

  val graphqlContext = new GraphqlContext(UserInfo("administrator", UserRole.Administrator), workspace)

  system.scheduler.scheduleWithFixedDelay(FiniteDuration(1, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.SECONDS))(new Runnable {
    override def run(): Unit = { buffer.flush() }
  })

  it should "send log records to distribution server" in {
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")

    Thread.sleep(5000)

    assertResult((OK,
      ("""{"data":{"serviceLogs":[""" +
        """{"distributionName":"test","serviceName":"service1","instanceId":"instance1","line":{"level":"INFO","message":"log line 1"}},""" +
        """{"distributionName":"test","serviceName":"service1","instanceId":"instance1","line":{"level":"WARN","message":"log line 2"}},""" +
        """{"distributionName":"test","serviceName":"service1","instanceId":"instance1","line":{"level":"ERROR","message":"log line 3"}},""" +
        """{"distributionName":"test","serviceName":"service1","instanceId":"instance1","line":{"level":"WARN","message":"log line 4"}},""" +
        """{"distributionName":"test","serviceName":"service1","instanceId":"instance1","line":{"level":"INFO","message":"log line 5"}}]}}""").parseJson))(
      result(graphql.executeQuery(GraphqlSchema.AdministratorSchemaDefinition, graphqlContext, graphql"""
        query {
          serviceLogs {
            distributionName
            serviceName
            instanceId
            line {
              level
              message
            }
          }
        }
      """))
    )
  }
}