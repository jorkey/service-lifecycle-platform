package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, ServicesProfileId}
import spray.json.DefaultJsonProtocol

case class DistributionConsumerInfo(distribution: DistributionId, profile: ServicesProfileId, testConsumer: Option[String])

object DistributionConsumerInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat3(DistributionConsumerInfo.apply)
}