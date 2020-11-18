package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{DistributionName, ProfileName}
import spray.json.DefaultJsonProtocol

case class ClientConfig(installProfile: ProfileName, testClientMatch: Option[String])

object ClientConfig extends DefaultJsonProtocol {
  implicit val clientConfigJson = jsonFormat2(ClientConfig.apply)
}

case class ClientInfo(distributionName: DistributionName, clientConfig: ClientConfig)

object ClientInfo extends DefaultJsonProtocol {
  implicit val clientInfoJson = jsonFormat2(ClientInfo.apply)
}
