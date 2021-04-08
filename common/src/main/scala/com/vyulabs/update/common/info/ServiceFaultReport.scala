package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionName, FaultId}
import spray.json.DefaultJsonProtocol

case class ServiceFaultReport(faultId: FaultId, info: FaultInfo, files: Seq[String])

object ServiceFaultReport extends DefaultJsonProtocol {
  implicit val serviceFaultInfoJson = jsonFormat3(ServiceFaultReport.apply)
}

case class DistributionFaultReport(distribution: DistributionName, report: ServiceFaultReport)

object DistributionFaultReport extends DefaultJsonProtocol {
  implicit val distributionFaultInfoJson = jsonFormat2(DistributionFaultReport.apply)
}
