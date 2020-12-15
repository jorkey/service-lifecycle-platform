package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

import java.util.Date

case class LogLine(date: Date, level: String, loggerName: Option[String], message: String)

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat4(LogLine.apply)
}

case class ServiceLogLine(distributionName: DistributionName, serviceName: ServiceName,
                          instanceId: InstanceId, processId: ProcessId, directory: ServiceDirectory, line: LogLine)