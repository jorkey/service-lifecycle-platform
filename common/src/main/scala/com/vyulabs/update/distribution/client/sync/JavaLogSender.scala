package com.vyulabs.update.distribution.client.sync

import ch.qos.logback.classic.{Logger}
import com.vyulabs.update.common.Common.{InstanceId, ServiceName}
import com.vyulabs.update.distribution.client.graphql.CommonMutationsCoder
import com.vyulabs.update.info.LogLine
import com.vyulabs.update.logger.{LogSender, LogSenderBuffer, LogTraceAppender}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import spray.json.DefaultJsonProtocol._

class JavaLogSender(serviceName: ServiceName, instanceId: InstanceId, client: SyncDistributionClient)
                   (implicit executionContext: ExecutionContext) extends LogSender {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  val processId = ProcessHandle.current.pid
  val directory = new java.io.File(".").getCanonicalPath()

  val appender = log.asInstanceOf[Logger].getAppender("TRACE").asInstanceOf[LogTraceAppender]
  val buffer = new LogSenderBuffer(this, 25, 1000)
  appender.addListener(buffer)

  override def sendLogLines(events: Seq[LogLine]): Future[Unit] = {
    Future(client.graphqlRequest(
      CommonMutationsCoder.addServiceLogs(serviceName, instanceId, processId.toString, directory, events))).map(_ => ())
  }
}
