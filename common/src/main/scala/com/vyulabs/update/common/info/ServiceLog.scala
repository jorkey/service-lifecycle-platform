package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

import java.util.Date

case class LogLine(time: Date, level: String, unit: String, message: String, terminationStatus: Option[Boolean])

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat5(LogLine.apply)
}

case class ServiceLogLine(service: ServiceId, instance: InstanceId, directory: ServiceDirectory, process: ProcessId,
                          task: Option[TaskId], payload: LogLine)

object ServiceLogLine extends DefaultJsonProtocol {
  implicit val serviceLogLineJson = jsonFormat6(ServiceLogLine.apply)
}

case class SequencedServiceLogLine(sequence: BigInt, instance: InstanceId, directory: ServiceDirectory, process: ProcessId,
                                   payload: LogLine)

object SequencedServiceLogLine extends DefaultJsonProtocol {
  implicit val sequencedServiceLogLineJson = jsonFormat5(SequencedServiceLogLine.apply)
}