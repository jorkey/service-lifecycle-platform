package com.vyulabs.update.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import org.slf4j.LoggerFactory

import ch.qos.logback.classic.{Level, Logger}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class LogTraceAppenderTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "Log trace appender"

  var messages = Seq.empty[String]

  val log: Logger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
  log.setLevel(Level.INFO)
  val appender = new LogTraceAppender()
  appender.addListener(new LogListener {
    override def append(event: ILoggingEvent): Unit = {
      messages :+= event.getMessage
    }
  })
  appender.start()
  log.addAppender(appender)

  it should "append log records" in {
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")
    assertResult(Seq("log line 1", "log line 2", "log line 3", "log line 4", "log line 5"))(messages)
  }
}
