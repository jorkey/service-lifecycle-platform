package com.vyulabs.update.common.logger

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceName, TaskId}
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.CommonMutationsCoder
import com.vyulabs.update.common.info.LogLine
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class LogUploader[Source[_]](serviceName: ServiceName, taskId: Option[TaskId], instanceId: InstanceId, client: DistributionClient[Source])
                            (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val processId = ProcessHandle.current.pid
  private val directory = new java.io.File(".").getCanonicalPath()

  override def receiveLogLines(lines: Seq[LogLine]): Future[Unit] = {
    client.graphqlRequest(
      CommonMutationsCoder.addServiceLogs(serviceName, instanceId, processId.toString, taskId, directory, lines)).map(_ => ())
  }
}
