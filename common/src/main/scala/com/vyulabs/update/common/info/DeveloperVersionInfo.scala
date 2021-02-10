package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import spray.json.DefaultJsonProtocol

import java.util.Date

case class BuildInfo(author: String, branches: Seq[String], date: Date, comment: Option[String])

object BuildInfo extends DefaultJsonProtocol {
  implicit val versionInfoJson = jsonFormat4(BuildInfo.apply)
}

case class DeveloperVersionInfo(serviceName: ServiceName, version: DeveloperDistributionVersion, buildInfo: BuildInfo)

object DeveloperVersionInfo extends DefaultJsonProtocol {
  implicit val serviceVersionInfoJson = jsonFormat3(DeveloperVersionInfo.apply)
}

case class DeveloperVersionsInfo(versions: Seq[DeveloperVersionInfo])

object DeveloperVersionsInfoJson extends DefaultJsonProtocol {
  implicit val developerVersionsInfoJson = jsonFormat1(DeveloperVersionsInfo.apply)
}

case class InstallInfo(user: String, date: Date)

object InstallInfo extends DefaultJsonProtocol {
  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}