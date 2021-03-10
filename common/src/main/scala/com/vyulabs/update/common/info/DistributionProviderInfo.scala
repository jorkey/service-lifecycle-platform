package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.DistributionName
import com.vyulabs.update.common.utils.JsonFormats._
import spray.json.DefaultJsonProtocol

import java.net.URL
import scala.concurrent.duration.FiniteDuration

case class DistributionProviderInfo(distributionName: DistributionName, distributionUrl: URL, uploadStateInterval: Option[FiniteDuration])

object DistributionProviderInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat3(DistributionProviderInfo.apply)
}

