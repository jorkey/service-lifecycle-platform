package com.vyulabs.update.logger

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase

trait LogListener {
  def append(event: ILoggingEvent): Unit
  def stop(): Unit
}

class LogTraceAppender extends AppenderBase[ILoggingEvent] {
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