package com.vyulabs.update.common.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.vyulabs.update.common.info.LogLine

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LogReceiver {
  def receiveLogLines(lines: Seq[LogLine]): Future[Unit]
}

class LogBuffer(logReceiver: LogReceiver, lowWater: Int, highWater: Int)
               (implicit executionContext: ExecutionContext) extends LogListener {
  private var eventsBuffer = Seq.empty[LogLine]
  private var sendingEvents = Seq.empty[LogLine]
  private var skipped = 0

  def append(event: ILoggingEvent): Unit = {
    synchronized {
      if (eventsBuffer.size + sendingEvents.size < highWater) {
        if (skipped != 0) {
          eventsBuffer :+= LogLine(new Date(), Level.ERROR.toString, Some("LogSender"),
            s"------------------------------ Skipped ${skipped} events ------------------------------", None)
          skipped = 0
        }
        eventsBuffer :+= LogLine(new Date(event.getTimeStamp), event.getLevel.toString, Some(event.getLoggerName), event.getMessage, None)
      } else {
        skipped += 1
      }
      maybeSend()
    }
  }

  override def stop(): Unit = {
    synchronized {
      eventsBuffer :+= LogLine(new Date(), "", None, "", Some(0))
    }
    flush()
  }

  def flush(): Unit = {
    synchronized {
      if (!eventsBuffer.isEmpty && sendingEvents.isEmpty) {
        send()
      }
    }
  }

  private def maybeSend(): Unit = {
    if (eventsBuffer.size >= lowWater && sendingEvents.isEmpty) {
      send()
    }
  }

  private def send(): Unit = {
    sendingEvents = eventsBuffer
    eventsBuffer = Seq.empty
    logReceiver.receiveLogLines(sendingEvents).onComplete {
      case Success(_) =>
        synchronized {
          sendingEvents = Seq.empty
          maybeSend()
        }
      case Failure(ex) =>
        synchronized {
          eventsBuffer = sendingEvents ++ eventsBuffer
          sendingEvents = Seq.empty
          maybeSend()
        }
    }
  }
}