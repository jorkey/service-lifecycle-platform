package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionName, ProfileName, ServiceName}
import spray.json.DefaultJsonProtocol

case class DistributionConsumerInfo(distributionName: DistributionName, installProfile: ProfileName, testDistributionMatch: Option[String])

object DistributionConsumerInfo extends DefaultJsonProtocol {
  implicit val infoJson = jsonFormat3(DistributionConsumerInfo.apply)
}

case class DistributionConsumerProfile(profileName: ProfileName, services: Set[ServiceName])

object DistributionConsumerProfile extends DefaultJsonProtocol {
  implicit val profileJson = jsonFormat2(DistributionConsumerProfile.apply)
}
