package com.vyulabs.update.config

import com.vyulabs.update.common.Common.InstallProfileName
import spray.json.DefaultJsonProtocol

import scala.util.matching.Regex

case class ClientConfig(installProfile: InstallProfileName, testClientMatch: Option[Regex])

object ClientConfigJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.RegexJson._

  implicit val clientInfoJson = jsonFormat2(ClientConfig.apply)
}
