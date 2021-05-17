package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.DistributionId
import com.vyulabs.update.common.utils.JsonFormats._
import spray.json.DefaultJsonProtocol

import java.net.URL

case class DistributionProviderInfo(distribution: DistributionId, url: URL, uploadStateIntervalSec: Option[Int])

object DistributionProviderInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat3(DistributionProviderInfo.apply)
}

