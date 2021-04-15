package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

import java.util.Date

case class LogLine(date: Date, level: String, unit: String, message: String, terminationStatus: Option[Boolean])

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat5(LogLine.apply)
}

case class ServiceLogLine(distribution: DistributionId, service: ServiceId,
                          taskId: Option[TaskId], instance: InstanceId, processId: ProcessId, directory: ServiceDirectory, line: LogLine)

object ServiceLogLine extends DefaultJsonProtocol {
  implicit val serviceLogLineJson = jsonFormat7(ServiceLogLine.apply)
}

case class SequencedServiceLogLine(sequence: Long, logLine: ServiceLogLine)

object SequencedServiceLogLine extends DefaultJsonProtocol {
  implicit val sequencedServiceLogLineJson = jsonFormat2(SequencedServiceLogLine.apply)
}
