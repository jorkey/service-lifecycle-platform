package com.vyulabs.update.info

import com.vyulabs.update.common.Common.DistributionName
import spray.json.DefaultJsonProtocol

case class DistributionInfo(title: String, distributionName: DistributionName)

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat2(DistributionInfo.apply)
}