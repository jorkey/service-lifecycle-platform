package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.DistributionId
import spray.json.DefaultJsonProtocol

case class DistributionInfo(distribution: DistributionId, title: String)

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat2(DistributionInfo.apply)
}