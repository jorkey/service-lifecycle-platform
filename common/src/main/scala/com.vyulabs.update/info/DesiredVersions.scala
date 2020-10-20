package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class DesiredVersions(versions: Map[ServiceName, BuildVersion])

object DesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DesiredVersions.apply)
}

case class ClientDesiredVersions(clientName: ClientName, desiredVersions: DesiredVersions)

object ClientDesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat2(ClientDesiredVersions.apply)
}