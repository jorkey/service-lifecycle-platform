package com.vyulabs.update.distribution.logger

import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId, ServiceName}
import com.vyulabs.update.distribution.mongo.{DatabaseCollections}
import com.vyulabs.update.common.info.{LogLine, ServiceLogLine}
import com.vyulabs.update.common.logger.LogReceiver
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LogStorer(distributionName: DistributionName, serviceName: ServiceName, instanceId: InstanceId, collections: DatabaseCollections)
               (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val processId = ProcessHandle.current.pid.toString
  private val directory = new java.io.File(".").getCanonicalPath()

  override def receiveLogLines(logs: Seq[LogLine]): Future[Unit] = {
    collections.State_ServiceLogs.insert(
      logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => { seq :+
        ServiceLogLine(distributionName, serviceName, instanceId, processId, directory, line) })).map(_ => ())
  }
}
