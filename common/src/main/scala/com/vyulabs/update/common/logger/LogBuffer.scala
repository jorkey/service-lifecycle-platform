package com.vyulabs.update.common.logger

import ch.qos.logback.classic.Level
import com.vyulabs.update.common.common.{Cancelable, Timer}
import com.vyulabs.update.common.info.LogLine

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait LogReceiver {
  def receiveLogLines(lines: Seq[LogLine]): Future[Unit]
}

class LogBuffer(description: String, unitName: String, logReceiver: LogReceiver, lowWater: Int, highWater: Int)
               (implicit timer: Timer, executionContext: ExecutionContext) extends LogListener {
  private var eventsBuffer = Seq.empty[LogLine]
  private var sendingEvents = Seq.empty[LogLine]
  private var skipped = 0
  private var timerTask = Option.empty[Cancelable]

  override def start(): Unit = {
    synchronized {
      eventsBuffer :+= LogLine(new Date, "INFO", unitName, s"`${description}` started", None)
      timerTask = Some(timer.schedulePeriodically(() => flush(), FiniteDuration(1, TimeUnit.SECONDS)))
    }
    flush()
  }

  def append(line: LogLine): Unit = {
    synchronized {
      if (eventsBuffer.size + sendingEvents.size < highWater) {
        if (skipped != 0) {
          eventsBuffer :+= LogLine(new Date(), Level.ERROR.toString, unitName,
            s"------------------------------ Skipped ${skipped} events ------------------------------", None)
          skipped = 0
        }
        eventsBuffer :+= line
      } else {
        skipped += 1
      }
      maybeSend()
    }
  }

  override def stop(status: Option[Boolean], error: Option[String]): Unit = {
    val stat = status match {
      case Some(true) => " successfully"
      case Some(false) => " with error" +
        (error match {
          case Some(error) => s" : ${error}"
          case None => ""
        })
      case None => ""
    }
    synchronized {
      eventsBuffer :+= LogLine(new Date(), "INFO", unitName, s"`${description}` finished${stat}", status)
      flush()
      timerTask.foreach(_.cancel())
      timerTask = None
    }
  }

  private def flush(): Unit = {
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