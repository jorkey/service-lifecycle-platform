package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.{DefaultJsonProtocol}

case class DesiredVersions(desiredVersions: Map[ServiceName, BuildVersion])

object DesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DesiredVersions.apply)
}