package com.vyulabs.update.config

import com.vyulabs.update.common.Common.ServiceName
import spray.json.DefaultJsonProtocol

case class InstallProfile(services: Set[ServiceName])

object InstallProfileJson extends DefaultJsonProtocol {
  implicit val installProfileJson = jsonFormat1(InstallProfile)
}
