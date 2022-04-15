package com.vyulabs.update.distribution.logger

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceId, TaskId}
import com.vyulabs.update.common.config.LogsConfig
import com.vyulabs.update.common.info.{LogLine, ServiceLogLine}
import com.vyulabs.update.common.logger.LogReceiver
import com.vyulabs.update.distribution.mongo.SequencedCollection
import org.slf4j.LoggerFactory

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

class LogStorekeeper(service: ServiceId, task: Option[TaskId], instance: InstanceId,
                     collection: SequencedCollection[ServiceLogLine], logsConfig: LogsConfig)
                    (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val maxLineSize = 10*1024*1024 // mongoDb maximum payload document size is 16777216

  private val process = ProcessHandle.current.pid.toString
  private val directory = new java.io.File(".").getCanonicalPath()

  override def receiveLogLines(logs: Seq[LogLine]): Future[Unit] = {
    synchronized {
      val expirationTimeout = if (task.isDefined) logsConfig.taskLogExpirationTimeout else logsConfig.serviceLogExpirationTimeout
      val expireTime = new Date(System.currentTimeMillis() + expirationTimeout.toMillis)
      collection.insert(logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => {
        val newLine = if (line.message.length > maxLineSize) {
          line.copy(message = line.message.substring(0, maxLineSize) + " ...")
        } else {
          line
        }
        seq :+ ServiceLogLine(
          service = service,
          instance = instance,
          directory = directory,
          process = process,
          task = task,
          time = newLine.time,
          level = newLine.level,
          unit = newLine.unit,
          message = newLine.message,
          terminationStatus = newLine.terminationStatus,
          expireTime
        )
      })).map(_ => ())
    }
  }
}
