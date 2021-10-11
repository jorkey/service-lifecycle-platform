package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, FaultId}
import com.vyulabs.update.common.info.ServiceFaultReport.jsonFormat3
import spray.json.DefaultJsonProtocol

case class FileInfo(path: String, length: Long)

object FileInfo extends DefaultJsonProtocol {
  implicit val fileInfoJson = jsonFormat2(FileInfo.apply)
}

case class ServiceFaultReport(id: FaultId, info: FaultInfo, files: Seq[FileInfo])

object ServiceFaultReport extends DefaultJsonProtocol {
  implicit val serviceFaultInfoJson = jsonFormat3(ServiceFaultReport.apply)
}

case class DistributionFaultReport(distribution: DistributionId, payload: ServiceFaultReport)

object DistributionFaultReport extends DefaultJsonProtocol {
  implicit val distributionFaultInfoJson = jsonFormat2(DistributionFaultReport.apply)
}
