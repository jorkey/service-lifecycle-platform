package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ClientName
import spray.json.DefaultJsonProtocol

case class ClientFaultReport(clientName: ClientName, reportDirectory: String, reportFiles: Seq[String], faultInfo: FaultInfo)

object ClientFaultReport extends DefaultJsonProtocol {
  implicit val clientFaultInfoJson = jsonFormat4(ClientFaultReport.apply)
}
