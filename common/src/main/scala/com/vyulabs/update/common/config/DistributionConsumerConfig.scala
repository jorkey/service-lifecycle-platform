package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName, ServiceName}
import spray.json.DefaultJsonProtocol

case class DistributionConsumerConfig(installProfile: ProfileName, testDistributionMatch: Option[String])

object DistributionConsumerConfig extends DefaultJsonProtocol {
  implicit val configJson = jsonFormat2(DistributionConsumerConfig.apply)
}

case class DistributionConsumerInfo(distributionName: DistributionName, config: DistributionConsumerConfig)

object DistributionConsumerInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat2(DistributionConsumerInfo.apply)
}

case class DistributionConsumerProfile(profileName: ProfileName, services: Set[ServiceName])

object DistributionConsumerProfile extends DefaultJsonProtocol {
  implicit val profileJson = jsonFormat2(DistributionConsumerProfile.apply)
}
