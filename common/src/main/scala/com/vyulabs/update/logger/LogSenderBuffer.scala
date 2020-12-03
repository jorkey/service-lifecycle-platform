package com.vyulabs.update.logger

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.vyulabs.update.info.LogLine

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LogSender {
  def sendLogLines(lines: Seq[LogLine]): Future[Unit]
}

class LogSenderBuffer(logSender: LogSender, lowWater: Int, highWater: Int)
                     (implicit executionContext: ExecutionContext) extends LogListener {
  var eventsBuffer = Seq.empty[LogLine]
  var sendingEvents = Seq.empty[LogLine]
  var skipped = 0

  def append(event: ILoggingEvent): Unit = {
    synchronized {
      if (eventsBuffer.size + sendingEvents.size < highWater) {
        if (skipped != 0) {
          eventsBuffer :+= LogLine(new Date(), Level.ERROR.toString, Some("LogSender"),
            s"------------------------------ Skipped ${skipped} events ------------------------------")
          skipped = 0
        }
        eventsBuffer :+= LogLine(new Date(event.getTimeStamp), event.getLevel.toString, Some(event.getLoggerName), event.getMessage)
      } else {
        skipped += 1
      }
      maybeSend()
    }
  }

  override def stop(): Unit = {
    synchronized {
      if (!eventsBuffer.isEmpty) {
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
    logSender.sendLogLines(sendingEvents).onComplete {
      case Success(_) =>
        synchronized {
          sendingEvents = Seq.empty
          maybeSend()
        }
      case Failure(_) =>
        synchronized {
          eventsBuffer = sendingEvents ++ eventsBuffer
          sendingEvents = Seq.empty
          maybeSend()
        }
    }
  }
}