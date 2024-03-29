package com.vyulabs.update.common.logger

import ch.qos.logback.classic.spi.{ILoggingEvent, ThrowableProxy}
import ch.qos.logback.core.AppenderBase
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.info.LogLine
import com.vyulabs.update.common.utils.Utils

import java.util.Date
import scala.concurrent.ExecutionContext
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

trait LogListener {
  def start(): Unit
  def append(line: LogLine): Unit
  def stop(status: Option[Boolean], error: Option[String]): Unit
}

class TraceAppender extends AppenderBase[ILoggingEvent] {
  var listeners = Set.empty[LogListener]
  var terminationStatus = Option.empty[Boolean]
  var terminationError = Option.empty[String]

  def addListener(listener: LogListener): Unit = {
    listeners += listener
  }

  override def start(): Unit = {
    super.start()
    listeners.foreach(_.start())
  }

  override def append(event: ILoggingEvent): Unit = {
    val message = if (event.getThrowableProxy != null && event.getThrowableProxy.isInstanceOf[ThrowableProxy]) {
      val proxy = event.getThrowableProxy.asInstanceOf[ThrowableProxy]
      val output = new ByteArrayOutputStream()
      val utf8 = StandardCharsets.UTF_8.name
      val ps = new PrintStream(output, true, utf8)
      proxy.getThrowable.printStackTrace(ps)
      ps.close()
      val exceptionMessage = output.toString(utf8).replaceAll("\t", "    ")
      event.getFormattedMessage + "\n" + exceptionMessage
    } else {
      event.getFormattedMessage
    }
    listeners.foreach(_.append(LogLine(new Date(event.getTimeStamp), event.getLevel.toString, event.getLoggerName, message, None)))
  }

  def setTerminationStatus(status: Boolean, error: Option[String]): Unit = {
    terminationStatus = Some(status)
    terminationError = error
  }

  override def stop(): Unit = {
    listeners.foreach(_.stop(terminationStatus, terminationError))
    super.stop()
  }
}

object TraceAppender {
  def handleLogs(description: String, unitName: String, logReceiver: LogReceiver)
                (implicit timer: Timer, executionContext: ExecutionContext): Unit = {
    val appender = Utils.getLogbackTraceAppender().getOrElse {
      sys.error("No logback trace appender")
    }
    val buffer = new LogBuffer(description, unitName, logReceiver, 25, 1000)
    appender.addListener(buffer)
    buffer.start()
  }
}