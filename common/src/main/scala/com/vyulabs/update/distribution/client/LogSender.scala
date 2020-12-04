package com.vyulabs.update.distribution.client

import ch.qos.logback.classic.Logger
import com.vyulabs.update.common.Common.{InstanceId, ServiceName}
import com.vyulabs.update.distribution.client.graphql.CommonMutationsCoder
import com.vyulabs.update.info.LogLine
import com.vyulabs.update.logger.{LogBuffer, LogReceiver, TraceAppender}
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class LogSender(serviceName: ServiceName, instanceId: InstanceId, client: DistributionClient)
               (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val processId = ProcessHandle.current.pid
  private val directory = new java.io.File(".").getCanonicalPath()

  private val appender = log.asInstanceOf[Logger].getAppender("TRACE").asInstanceOf[TraceAppender]
  private val buffer = new LogBuffer(this, 25, 1000)

  appender.addListener(buffer)

  override def receiveLogLines(events: Seq[LogLine]): Future[Unit] = {
    Future(client.graphqlRequest(
      CommonMutationsCoder.addServiceLogs(serviceName, instanceId, processId.toString, directory, events))).map(_ => ())
  }

  def getBuffer() = buffer
}
