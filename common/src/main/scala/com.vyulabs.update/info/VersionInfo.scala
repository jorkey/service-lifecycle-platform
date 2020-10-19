package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class BuildVersionInfo(author: String, branches: Seq[String], date: Date, comment: Option[String])

object BuildVersionInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._

  implicit val versionInfoJson = jsonFormat4(BuildVersionInfo.apply)
}

case class VersionInfo(serviceName: ServiceName, clientName: Option[ClientName], version: BuildVersion, buildInfo: BuildVersionInfo)

object VersionInfo extends DefaultJsonProtocol {
  implicit val serviceVersionInfoJson = jsonFormat4(VersionInfo.apply)
}

case class VersionsInfo(versions: Seq[VersionInfo])

object VersionsInfoJson extends DefaultJsonProtocol {
  implicit val versionsInfoJson = jsonFormat1(VersionsInfo.apply)
}