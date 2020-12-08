package distribution.logger

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ServiceName}
import com.vyulabs.update.info.{LogLine, ServiceLogLine}
import com.vyulabs.update.logger.LogReceiver
import distribution.mongo.{DatabaseCollections, ServiceLogLineDocument}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class LogStorer(distributionName: DistributionName, serviceName: ServiceName, instanceId: InstanceId, collections: DatabaseCollections)
               (implicit executionContext: ExecutionContext) extends LogReceiver {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val processId = ProcessHandle.current.pid.toString
  private val directory = new java.io.File(".").getCanonicalPath()

  override def receiveLogLines(logs: Seq[LogLine]): Future[Unit] = {
    for {
      collection <- collections.State_ServiceLogs
      id <- collections.getNextSequence(collection.getName(), logs.size)
      result <- collection.insert(
        logs.foldLeft(Seq.empty[ServiceLogLineDocument])((seq, line) => { seq :+
          ServiceLogLineDocument(id - (logs.size-seq.size) + 1,
            new ServiceLogLine(distributionName, serviceName, instanceId, processId, directory, line)) })).map(_ => ())
    } yield result
  }
}
