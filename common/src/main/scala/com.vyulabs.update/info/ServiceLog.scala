package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common._
import com.vyulabs.update.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

// TODO graphql add log level
case class LogLine(date: Date, line: String)

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat2(LogLine.apply)
}

case class ServiceLogLine(serviceName: ServiceName, instanceId: InstanceId, directory: ServiceDirectory, logLine: LogLine)

case class ClientServiceLogLine(distributionName: DistributionName, logLine: ServiceLogLine)