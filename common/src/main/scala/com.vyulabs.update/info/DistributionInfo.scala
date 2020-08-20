package com.vyulabs.update.info

import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class DistributionInfo(name: String, version: BuildVersion)

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat2(DistributionInfo.apply)
}