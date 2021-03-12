package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ConsumerProfile, DistributionName, ServiceName}
import spray.json.DefaultJsonProtocol

case class DistributionConsumerInfo(distributionName: DistributionName, consumerProfile: ConsumerProfile, testDistributionMatch: Option[String])

object DistributionConsumerInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat3(DistributionConsumerInfo.apply)
}

case class DistributionConsumerProfile(consumerProfile: ConsumerProfile, services: Seq[ServiceName])

object DistributionConsumerProfile extends DefaultJsonProtocol {
  implicit val profileJson = jsonFormat2(DistributionConsumerProfile.apply)
}
