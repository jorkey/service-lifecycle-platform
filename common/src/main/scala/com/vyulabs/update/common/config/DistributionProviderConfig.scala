package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.DistributionName
import com.vyulabs.update.common.utils.JsonFormats._
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

import java.net.URL
import scala.concurrent.duration.FiniteDuration

case class DistributionProviderConfig(distributionUrl: URL, uploadStateInterval: Option[FiniteDuration])

object DistributionProviderConfig {
  implicit val configJson = jsonFormat2(DistributionProviderConfig.apply)
}

case class DistributionProviderInfo(distributionName: DistributionName, config: DistributionProviderConfig)

object DistributionProviderInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat2(DistributionProviderInfo.apply)
}

