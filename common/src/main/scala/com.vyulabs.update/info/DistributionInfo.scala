package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class DistributionInfo(name: String, version: BuildVersion, client: Option[ClientName])

object DistributionInfo extends DefaultJsonProtocol {
  implicit val serverInfoJson = jsonFormat3(DistributionInfo.apply)
}