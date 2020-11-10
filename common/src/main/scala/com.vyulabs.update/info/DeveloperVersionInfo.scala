package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class BuildInfo(author: String, branches: Seq[String], date: Date, comment: Option[String])

object BuildInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._

  implicit val versionInfoJson = jsonFormat4(BuildInfo.apply)
}

case class DeveloperVersionInfo(serviceName: ServiceName, clientName: Option[ClientName], version: BuildVersion, buildInfo: BuildInfo)

object DeveloperVersionInfo extends DefaultJsonProtocol {
  implicit val serviceVersionInfoJson = jsonFormat4(DeveloperVersionInfo.apply)
}

case class DeveloperVersionsInfo(versions: Seq[DeveloperVersionInfo])

object DeveloperVersionsInfoJson extends DefaultJsonProtocol {
  implicit val developerVersionsInfoJson = jsonFormat1(DeveloperVersionsInfo.apply)
}

case class InstallInfo(user: String, date: Date)

object InstallInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._

  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}