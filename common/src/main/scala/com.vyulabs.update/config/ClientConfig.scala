package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{ClientName, ProfileName}
import spray.json.DefaultJsonProtocol

import scala.util.matching.Regex

case class ClientConfig(installProfile: ProfileName, testClientMatch: Option[Regex])

object ClientConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.RegexJson._

  implicit val clientConfigJson = jsonFormat2(ClientConfig.apply)
}

case class ClientInfo(name: ClientName, installProfile: ProfileName, testClientMatch: Option[Regex])

object ClientInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.RegexJson._

  implicit val clientInfoJson = jsonFormat3(ClientInfo.apply)
}
