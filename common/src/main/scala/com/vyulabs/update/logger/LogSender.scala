package com.vyulabs.update.logger

import com.vyulabs.update.common.Common.{InstanceId, ServiceName}
import com.vyulabs.update.distribution.graphql.DistributionClient
import com.vyulabs.update.distribution.graphql.graphql.CommonMutationsCoder
import com.vyulabs.update.info.LogLine
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class LogSender(serviceName: ServiceName, instanceId: InstanceId, client: DistributionClient)
               (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val processId = ProcessHandle.current.pid
  private val directory = new java.io.File(".").getCanonicalPath()

  override def receiveLogLines(events: Seq[LogLine]): Future[Unit] = {
    client.graphqlRequest(
      CommonMutationsCoder.addServiceLogs(serviceName, instanceId, processId.toString, directory, events)).map(_ => ())
  }
}
