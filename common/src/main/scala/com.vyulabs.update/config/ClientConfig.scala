package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{ClientName, ProfileName}
import spray.json.DefaultJsonProtocol

case class ClientConfig(installProfile: ProfileName, testClientMatch: Option[String])

object ClientConfig extends DefaultJsonProtocol {
  implicit val clientConfigJson = jsonFormat2(ClientConfig.apply)
}

case class ClientInfo(name: ClientName, installProfile: ProfileName, testClientMatch: Option[String])

object ClientInfo extends DefaultJsonProtocol {
  implicit val clientInfoJson = jsonFormat3(ClientInfo.apply)
}
