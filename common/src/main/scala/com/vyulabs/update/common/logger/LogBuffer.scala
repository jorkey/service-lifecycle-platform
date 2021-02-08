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

class LogBuffer(description: String, loggerName: String,
                logReceiver: LogReceiver, lowWater: Int, highWater: Int)
               (implicit executionContext: ExecutionContext) extends LogListener {
  private var eventsBuffer = Seq.empty[LogLine]
  private var sendingEvents = Seq.empty[LogLine]
  private var skipped = 0

  override def start(): Unit = {
    synchronized {
      eventsBuffer :+= LogLine(new Date, "INFO", Some(loggerName), s"Logger `${description}` started", None)
    }
    flush()
  }

  def append(event: ILoggingEvent): Unit = {
    synchronized {
      if (eventsBuffer.size + sendingEvents.size < highWater) {
        if (skipped != 0) {
          eventsBuffer :+= LogLine(new Date(), Level.ERROR.toString, Some(loggerName),
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

  override def stop(status: Option[Boolean], error: Option[String]): Unit = {
    val stat = status match {
      case Some(true) => "successfully"
      case Some(false) => "with error" +
        (error match {
          case Some(error) => s": ${error}"
          case None => ""
        })
      case None => ""
    }
    synchronized {
      eventsBuffer :+= LogLine(new Date(), "INFO", Some(loggerName), s"Logger `${description}` finished ${stat}", status)
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