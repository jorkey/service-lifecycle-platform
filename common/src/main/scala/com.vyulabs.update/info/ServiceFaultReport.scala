package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{DistributionName, FaultId}
import spray.json.DefaultJsonProtocol

case class ServiceFaultReport(faultId: FaultId, info: FaultInfo, files: Seq[String])

object ServiceFaultReport extends DefaultJsonProtocol {
  implicit val serviceFaultInfoJson = jsonFormat3(ServiceFaultReport.apply)
}

case class DistributionFaultReport(distributionName: DistributionName, report: ServiceFaultReport)

object DistributionFaultReport extends DefaultJsonProtocol {
  implicit val distributionFaultInfoJson = jsonFormat2(DistributionFaultReport.apply)
}
