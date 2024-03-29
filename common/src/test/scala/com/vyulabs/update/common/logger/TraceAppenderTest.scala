package com.vyulabs.update.common.logger

import ch.qos.logback.classic.Level
import com.vyulabs.update.common.info.LogLine
import com.vyulabs.update.common.utils.Utils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, OneInstancePerTest}

class TraceAppenderTest extends FlatSpec with Matchers with BeforeAndAfterAll with OneInstancePerTest {
  behavior of "Log trace appender"

  var messages = Seq.empty[String]

  it should "find global appender" in {
    val log = Utils.getLogbackLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    val traceAppender = log.getAppender("TRACE").asInstanceOf[TraceAppender]
    assert(traceAppender != null)
  }

  it should "add appender to local logger and log records" in {
    val log = Utils.getLogbackLogger(this.getClass)
    log.setLevel(Level.INFO)
    val appender = new TraceAppender()
    appender.addListener(new LogListener {
      override def start(): Unit = {}
      override def append(line: LogLine): Unit =
        messages :+= line.message
      override def stop(status: Option[Boolean], error: Option[String]): Unit = {}
    })
    appender.start()
    log.addAppender(appender)

    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    log.warn("log line 4")
    log.info("log line 5")
    assertResult(Seq("log line 1", "log line 2", "log line 3", "log line 4", "log line 5"))(messages)
  }
}
