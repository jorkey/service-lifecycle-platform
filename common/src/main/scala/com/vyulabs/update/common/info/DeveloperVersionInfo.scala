package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol
import java.util.Date

case class BuildInfo(author: String, branches: Seq[String], date: Date, comment: Option[String])

object BuildInfo extends DefaultJsonProtocol {
  implicit val versionInfoJson = jsonFormat4(BuildInfo.apply)
}

case class DeveloperVersionInfo(serviceName: ServiceName, distributionName: DistributionName,
                                version: DeveloperVersion, buildInfo: BuildInfo)

object DeveloperVersionInfo extends DefaultJsonProtocol {
  implicit val serviceVersionInfoJson = jsonFormat4(DeveloperVersionInfo.apply)

  def from(serviceName: ServiceName, version: DeveloperDistributionVersion, buildInfo: BuildInfo): DeveloperVersionInfo = {
    new DeveloperVersionInfo(serviceName, version.distributionName, version.version, buildInfo)
  }
}

case class DeveloperVersionsInfo(versions: Seq[DeveloperVersionInfo])

object DeveloperVersionsInfoJson extends DefaultJsonProtocol {
  implicit val developerVersionsInfoJson = jsonFormat1(DeveloperVersionsInfo.apply)
}

case class InstallInfo(user: String, date: Date)

object InstallInfo extends DefaultJsonProtocol {
  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}