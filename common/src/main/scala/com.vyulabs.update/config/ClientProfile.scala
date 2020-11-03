package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{ProfileName, ServiceName}
import spray.json.DefaultJsonProtocol

case class ClientProfile(profileName: ProfileName, services: Set[ServiceName])

object ClientProfile extends DefaultJsonProtocol {
  implicit val installProfileJson = jsonFormat2(ClientProfile.apply)
}
