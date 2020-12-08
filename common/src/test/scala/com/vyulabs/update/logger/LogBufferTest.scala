package com.vyulabs.update.logger

import ch.qos.logback.classic.{Level, Logger}
import com.vyulabs.update.utils.Utils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, OneInstancePerTest}
import org.slf4j.LoggerFactory

import java.io.IOException
import scala.concurrent.Promise

class LogBufferTest extends FlatSpec with Matchers with BeforeAndAfterAll with OneInstancePerTest {
  behavior of "Log trace appender"

  implicit val executionContext = scala.concurrent.ExecutionContext.global
  implicit val log = Utils.getLogbackLogger(this.getClass)

  var messages = Seq.empty[String]
  var promise = Promise[Unit]

  log.setLevel(Level.INFO)
  val appender = new TraceAppender()
  val buffer = new LogBuffer(events => { messages = events.map(_.message); promise.future }, 3, 6)
  appender.addListener(buffer)
  appender.start()
  log.addAppender(appender)

  it should "buffer log records up to low water mark" in {
    promise = Promise[Unit]
    log.info("log line 1")
    getMessages(Seq())
    log.warn("log line 2")
    log.error("log line 3")
    getMessages(Seq("log line 1", "log line 2", "log line 3"))
    resetPromise()
    messages = Seq.empty
    log.warn("log line 4")
    log.info("log line 5")
    log.info("log line 6")
    getMessages(Seq("log line 4", "log line 5", "log line 6"))
    resetPromise()
  }

  it should "buffer log records while sending in process" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    getMessages(Seq("log line 1", "log line 2", "log line 3"))
    messages = Seq.empty
    log.warn("log line 4")
    log.info("log line 5")
    log.info("log line 6")
    resetPromise()
    getMessages(Seq("log line 4", "log line 5", "log line 6"))
    resetPromise()
  }

  it should "skip new log records when reached high water mark" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    getMessages(Seq("log line 1", "log line 2", "log line 3"))
    messages = Seq.empty
    log.warn("log line 4")
    log.info("log line 5")
    log.info("log line 6")
    log.info("log line 7")
    log.info("log line 8")
    resetPromise()
    getMessages(Seq("log line 4", "log line 5", "log line 6"))
    resetPromise()
    log.info("log line 9")
    log.warn("log line 10")
    getMessages(Seq("------------------------------ Skipped 2 events ------------------------------", "log line 9", "log line 10"))
    resetPromise()
  }

  it should "resend log records after error" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    getMessages(Seq("log line 1", "log line 2", "log line 3"))
    log.warn("log line 4")
    log.info("log line 5")
    resetPromiseWithError()
    getMessages(Seq("log line 1", "log line 2", "log line 3", "log line 4", "log line 5"))
    resetPromise()
  }

  it should "flush buffer by request" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    getMessages(Seq())
    buffer.flush()
    getMessages(Seq("log line 1", "log line 2"))
  }

  it should "not flush buffer when sending in process" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    log.error("log line 3")
    getMessages(Seq("log line 1", "log line 2", "log line 3"))
    log.warn("log line 4")
    log.info("log line 5")
    buffer.flush()
    getMessages(Seq())
  }

  it should "flush buffer when appender stopped" in {
    messages = Seq.empty
    promise = Promise[Unit]
    log.info("log line 1")
    log.warn("log line 2")
    getMessages(Seq())
    appender.stop()
    getMessages(Seq("log line 1", "log line 2"))
  }

  private def resetPromise(): Unit = {
    val p = promise
    promise = Promise[Unit]
    p.success()
  }

  private def resetPromiseWithError(): Unit = {
    val p = promise
    promise = Promise[Unit]
    p.failure(new IOException("error"))
  }

  private def getMessages(waitingMessages: Seq[String]): Unit = {
    Thread.sleep(100)
    assertResult(waitingMessages)(messages)
    messages = Seq.empty
  }
}
