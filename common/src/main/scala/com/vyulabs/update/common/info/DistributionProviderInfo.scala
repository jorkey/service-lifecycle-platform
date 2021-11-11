package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId}
import spray.json.DefaultJsonProtocol

case class DistributionProviderInfo(distribution: DistributionId, url: String, accessToken: String,
                                    testConsumer: Option[String], uploadStateIntervalSec: Option[Int],
                                    autoUpdate: Option[Boolean])

object DistributionProviderInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat6(DistributionProviderInfo.apply)
}

