package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{ProfileName, ServiceName}
import spray.json.DefaultJsonProtocol

case class DistributionClientProfile(profileName: ProfileName, services: Set[ServiceName])

object DistributionClientProfile extends DefaultJsonProtocol {
  implicit val distributionClientProfileJson = jsonFormat2(DistributionClientProfile.apply)
}
