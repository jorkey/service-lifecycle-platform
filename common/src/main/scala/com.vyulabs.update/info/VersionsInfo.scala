package com.vyulabs.update.info

import spray.json.DefaultJsonProtocol

case class VersionsInfo(versions: Seq[VersionInfo])

object VersionsInfoJson extends DefaultJsonProtocol {
  import VersionInfo._

  implicit val versionsInfoJson = jsonFormat1(VersionsInfo.apply)
}