package com.vyulabs.update.distribution.logger

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceId, TaskId}
import com.vyulabs.update.common.info.{LogLine, ServiceLogLine}
import com.vyulabs.update.common.logger.LogReceiver
import com.vyulabs.update.distribution.mongo.SequencedCollection
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LogStorekeeper(service: ServiceId, task: Option[TaskId], instance: InstanceId,
                     collection: SequencedCollection[ServiceLogLine])
                    (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val maxLineSize = 10*1024*1024 // mongoDb maximum payload document size is 16777216

  private val process = ProcessHandle.current.pid.toString
  private val directory = new java.io.File(".").getCanonicalPath()

  private var logOutputFuture = Option.empty[Future[Unit]]

  override def receiveLogLines(logs: Seq[LogLine]): Future[Unit] = {
    synchronized {
      logOutputFuture = Some(logOutputFuture.getOrElse(Future()).flatMap { _ =>
        collection.insert(logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => {
          val newLine = if (line.message.length > maxLineSize) {
            line.copy(message = line.message.substring(0, maxLineSize) + " ...")
          } else {
            line
          }
          seq :+ ServiceLogLine(service, instance, directory, process, task, newLine)
        })).map(_ => ())
      })
      logOutputFuture.get
    }
  }
}
