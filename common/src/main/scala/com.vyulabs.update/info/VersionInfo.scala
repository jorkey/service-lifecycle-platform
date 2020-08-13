package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class VersionInfo(version: BuildVersion, author: String, branches: Seq[String], date: Date, comment: Option[String])

object VersionInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersionJson._

  implicit val versionInfoJson = jsonFormat5(VersionInfo.apply)
}