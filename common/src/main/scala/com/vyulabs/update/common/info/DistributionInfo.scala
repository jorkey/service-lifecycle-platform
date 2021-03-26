package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.DistributionName
import spray.json.DefaultJsonProtocol

case class DistributionInfo(distributionName: DistributionName, title: String)

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat2(DistributionInfo.apply)
}