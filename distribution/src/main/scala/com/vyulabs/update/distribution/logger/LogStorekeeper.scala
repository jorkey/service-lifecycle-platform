package com.vyulabs.update.distribution.logger

import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId, ServiceId, TaskId}
import com.vyulabs.update.common.info.{LogLine, ServiceLogLine}
import com.vyulabs.update.common.logger.LogReceiver
import com.vyulabs.update.distribution.mongo.SequencedCollection
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LogStorekeeper(service: ServiceId, task: Option[TaskId], instance: InstanceId,
                     collection: SequencedCollection[ServiceLogLine])
                    (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val process = ProcessHandle.current.pid.toString
  private val directory = new java.io.File(".").getCanonicalPath()

  @volatile private var logOutputFuture = Option.empty[Future[Unit]]

  override def receiveLogLines(logs: Seq[LogLine]): Future[Unit] = {
    logOutputFuture = Some(logOutputFuture.getOrElse(Future()).flatMap { _ =>
      collection.insert(logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => {
        seq :+ ServiceLogLine(service, task, instance, process, directory, line)
      })).map(_ => ())
    })
    logOutputFuture.get
  }
}
