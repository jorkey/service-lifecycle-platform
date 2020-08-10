package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class ServicesVersions(servicesVersions: Map[ServiceName, BuildVersion])

object ServicesVersionsJson extends DefaultJsonProtocol {
  import com.vyulabs.update.version.BuildVersionJson._

  implicit val servicesVersionsJson = jsonFormat1(ServicesVersions)
}