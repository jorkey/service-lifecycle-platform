package com.vyulabs.update.config

import com.vyulabs.update.common.Common.{ProfileName, ServiceName}
import spray.json.DefaultJsonProtocol

case class InstallProfile(profileName: ProfileName, services: Set[ServiceName])

object InstallProfile extends DefaultJsonProtocol {
  implicit val installProfileJson = jsonFormat2(InstallProfile.apply)
}
