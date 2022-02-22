package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.DistributionId
import com.vyulabs.update.common.version.ClientDistributionVersion
import spray.json.DefaultJsonProtocol

case class DistributionInfo(distribution: DistributionId, title: String, version: ClientDistributionVersion)

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat3(DistributionInfo.apply)
}