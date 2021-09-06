package com.vyulabs.update.common.logger

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceId, TaskId}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.AddServiceLogsCoder
import com.vyulabs.update.common.info.LogLine
import org.slf4j.helpers.NOPLogger
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class LogUploader[Source[_]](service: ServiceId, task: Option[TaskId], instance: InstanceId, client: DistributionClient[Source])
                            (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = NOPLogger.NOP_LOGGER

  private val process = ProcessHandle.current.pid
  private val directory = new java.io.File(".").getCanonicalPath()

  private val coder = new AddServiceLogsCoder() {}

  override def receiveLogLines(lines: Seq[LogLine]): Future[Unit] = {
    client.graphqlRequest(
      coder.addServiceLogs(service, instance, process.toString, task, directory, lines)).map(_ => ())
  }
}
