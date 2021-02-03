package com.vyulabs.update.common.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.utils.Utils

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait LogListener {
  def start(): Unit
  def append(event: ILoggingEvent): Unit
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
    listeners.foreach(_.append(event))
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
  def handleLogs(description: String, loggerName: String, logReceiver: LogReceiver)(implicit timer: Timer, executionContext: ExecutionContext): Unit = {
    val logger = Utils.getLogbackLogger(this.getClass)
    val appender = logger.getAppender("TRACE").asInstanceOf[TraceAppender]
    val buffer = new LogBuffer(description, loggerName, logReceiver, 25, 1000)
    appender.addListener(buffer)
    timer.schedulePeriodically(() => buffer.flush(), FiniteDuration(1, TimeUnit.SECONDS))
  }
}