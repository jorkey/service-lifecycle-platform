package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{DistributionName, FaultId}
import spray.json.DefaultJsonProtocol

case class ClientFaultReport(faultId: FaultId, distributionName: DistributionName, info: FaultInfo, files: Seq[String])

object ClientFaultReport extends DefaultJsonProtocol {
  implicit val clientFaultInfoJson = jsonFormat4(ClientFaultReport.apply)
}
