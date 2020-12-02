package com.vyulabs.update.distribution.logger

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import ch.qos.logback.classic.spi.ILoggingEvent
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.logger.{LogListener, LogTraceAppender}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import ch.qos.logback.classic.{Level, Logger}

class LogTraceAppenderTest extends TestEnvironment {
  behavior of "Log trace appender"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  var messages = Seq.empty[String]

  val logger: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
  logger.setLevel(Level.INFO)
  val appender = new LogTraceAppender()
  appender.addListener(new LogListener {
    override def append(event: ILoggingEvent): Unit = {
      messages :+= event.getMessage
    }
  })
  appender.start()
  logger.addAppender(appender)

  it should "append log records" in {
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")
    assertResult(Seq("log line 1", "log line 2", "log line 3", "log line 4", "log line 5"))(messages)
  }
}
