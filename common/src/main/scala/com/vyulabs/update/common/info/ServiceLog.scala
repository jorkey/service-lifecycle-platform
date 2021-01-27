package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

import java.util.Date

case class LogLine(date: Date, level: String, loggerName: Option[String], message: String, eof: Option[Boolean])

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat5(LogLine.apply)
}

case class ServiceLogLine(distributionName: DistributionName, serviceName: ServiceName,
                          instanceId: InstanceId, processId: ProcessId, taskId: Option[TaskId], directory: ServiceDirectory, line: LogLine)