package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

import java.util.Date

case class LogLine(time: Date, level: String, unit: String, message: String, terminationStatus: Option[Boolean])

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat5(LogLine.apply)
}

case class ServiceLogLine(service: ServiceId, instance: InstanceId, directory: ServiceDirectory, process: ProcessId, task: Option[TaskId],
                          time: Date, level: String, unit: String, message: String, terminationStatus: Option[Boolean], expireTime: Date)

object ServiceLogLine extends DefaultJsonProtocol {
  implicit val serviceLogLineJson = jsonFormat11(ServiceLogLine.apply)
}

case class SequencedServiceLogLine(sequence: BigInt, service: ServiceId, instance: InstanceId, directory: ServiceDirectory,
                                   process: ProcessId, task: Option[TaskId], time: Date, level: String, unit: String,
                                   message: String, terminationStatus: Option[Boolean]) {
  def getLogLine() = LogLine(time, level, unit, message, terminationStatus)
}

object SequencedServiceLogLine extends DefaultJsonProtocol {
  implicit val sequencedServiceLogLineJson = jsonFormat11(SequencedServiceLogLine.apply)
}