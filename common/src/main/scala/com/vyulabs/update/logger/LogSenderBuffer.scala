package com.vyulabs.update.logger

import ch.qos.logback.classic.spi.ILoggingEvent

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LogSender {
  def sendLogEvents(events: Seq[ILoggingEvent]): Future[Unit]
}

class LogSenderBuffer(logSender: LogSender, lowWater: Int, highWater: Int)
                     (implicit executionContext: ExecutionContext) extends LogListener {
  var eventsBuffer = Seq.empty[ILoggingEvent]
  var sendingEvents = Seq.empty[ILoggingEvent]

  def append(event: ILoggingEvent): Unit = {
    synchronized {
      if (eventsBuffer.size + sendingEvents.size < highWater) {
        eventsBuffer :+= event
      }
      maybeSend()
    }
  }

  private def maybeSend(): Unit = {
    if (eventsBuffer.size >= lowWater && sendingEvents.isEmpty) {
      sendingEvents = eventsBuffer
      eventsBuffer = Seq.empty
      logSender.sendLogEvents(sendingEvents).onComplete {
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
}
