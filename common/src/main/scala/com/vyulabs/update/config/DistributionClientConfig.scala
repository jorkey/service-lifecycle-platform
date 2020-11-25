package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{DistributionName, ProfileName}
import spray.json.DefaultJsonProtocol

case class DistributionClientConfig(installProfile: ProfileName, testDistributionMatch: Option[String])

object DistributionClientConfig extends DefaultJsonProtocol {
  implicit val clientConfigJson = jsonFormat2(DistributionClientConfig.apply)
}

case class DistributionClientInfo(distributionName: DistributionName, clientConfig: DistributionClientConfig)

object DistributionClientInfo extends DefaultJsonProtocol {
  implicit val clientInfoJson = jsonFormat2(DistributionClientInfo.apply)
}
