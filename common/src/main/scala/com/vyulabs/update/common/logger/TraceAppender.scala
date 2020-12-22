package com.vyulabs.update.common.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.utils.Utils

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

trait LogListener {
  def append(event: ILoggingEvent): Unit
  def stop(): Unit
}

class TraceAppender extends AppenderBase[ILoggingEvent] {
  var listeners = Set.empty[LogListener]

  def addListener(listener: LogListener): Unit = {
    listeners += listener
  }

  override def append(event: ILoggingEvent): Unit = {
    listeners.foreach(_.append(event))
  }

  override def stop(): Unit = {
    listeners.foreach(_.stop())
    super.stop()
  }
}

object TraceAppender {
  def handleLogs(logReceiver: LogReceiver)(implicit timer: Timer, executionContext: ExecutionContext): Unit = {
    val logger = Utils.getLogbackLogger(this.getClass)
    val appender = logger.getAppender("TRACE").asInstanceOf[TraceAppender]
    val buffer = new LogBuffer(logReceiver, 25, 1000)
    appender.addListener(buffer)
    timer.schedulePeriodically(() => buffer.flush(), FiniteDuration(1, TimeUnit.SECONDS))
  }
}