package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, FaultId}
import spray.json.DefaultJsonProtocol

case class ServiceFaultReport(fault: FaultId, info: FaultInfo, files: Seq[FileInfo])

object ServiceFaultReport extends DefaultJsonProtocol {
  implicit val serviceFaultInfoJson = jsonFormat3(ServiceFaultReport.apply)
}

case class DistributionFaultReport(distribution: DistributionId, fault: FaultId, info: FaultInfo, files: Seq[FileInfo])

object DistributionFaultReport extends DefaultJsonProtocol {
  implicit val distributionFaultInfoJson = jsonFormat4(DistributionFaultReport.apply)
}
