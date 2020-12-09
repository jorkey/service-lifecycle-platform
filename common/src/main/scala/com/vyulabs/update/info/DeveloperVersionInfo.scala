package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.version.{DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol

case class BuildInfo(author: String, branches: Seq[String], date: Date, comment: Option[String])

object BuildInfo extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._

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
  import com.vyulabs.update.utils.Utils.DateJson._

  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}