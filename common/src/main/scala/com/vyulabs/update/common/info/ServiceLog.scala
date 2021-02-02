package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

import java.util.Date

case class LogLine(date: Date, level: String, loggerName: Option[String], message: String, exitCode: Option[Int])

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat5(LogLine.apply)
}

case class ServiceLogLine(distributionName: DistributionName, serviceName: ServiceName,
                          taskId: Option[TaskId], instanceId: InstanceId, processId: ProcessId, directory: ServiceDirectory, line: LogLine)

object ServiceLogLine extends DefaultJsonProtocol {
  implicit val serviceLogLineJson = jsonFormat7(ServiceLogLine.apply)
}

case class SequencedServiceLogLine(sequence: Long, logLine: ServiceLogLine)

object SequencedServiceLogLine extends DefaultJsonProtocol {
  implicit val sequencedServiceLogLineJson = jsonFormat2(SequencedServiceLogLine.apply)
}
