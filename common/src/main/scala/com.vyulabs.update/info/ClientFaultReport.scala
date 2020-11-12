package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ClientName, FaultId}
import spray.json.DefaultJsonProtocol

case class ClientFaultReport(faultId: FaultId, clientName: ClientName, info: FaultInfo, files: Seq[String])

object ClientFaultReport extends DefaultJsonProtocol {
  implicit val clientFaultInfoJson = jsonFormat4(ClientFaultReport.apply)
}
