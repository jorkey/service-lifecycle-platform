package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common._
import com.vyulabs.update.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

case class LogLine(date: Date, line: String)

object LogLine extends DefaultJsonProtocol {
  implicit val logLineJson = jsonFormat2(LogLine.apply)
}

case class ClientServiceLogLine(clientName: ClientName, serviceName: ServiceName, instanceId: InstanceId, directory: ServiceDirectory, logLine: LogLine)

object ClientServiceLogLine extends DefaultJsonProtocol {
  implicit val clientLogLineJson = jsonFormat5(ClientServiceLogLine.apply)
}
