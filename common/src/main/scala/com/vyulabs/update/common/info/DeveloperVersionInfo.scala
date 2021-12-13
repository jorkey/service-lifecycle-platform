package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServiceId, TaskId, AccountId}
import com.vyulabs.update.common.config.Repository
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol

import java.util.Date

case class BuildInfo(author: String, sources: Seq[Repository], time: Date, comment: String)

object BuildInfo extends DefaultJsonProtocol {
  implicit val versionInfoJson = jsonFormat4(BuildInfo.apply)
}

case class DeveloperVersionInfo(service: ServiceId, version: DeveloperDistributionVersion, buildInfo: BuildInfo)

object DeveloperVersionInfo extends DefaultJsonProtocol {
  implicit val serviceVersionInfoJson = jsonFormat3(DeveloperVersionInfo.apply)

  def from(service: ServiceId, version: DeveloperDistributionVersion, buildInfo: BuildInfo): DeveloperVersionInfo = {
    new DeveloperVersionInfo(service, version, buildInfo)
  }
}

case class DeveloperVersionsInfo(versions: Seq[DeveloperVersionInfo])

object DeveloperVersionsInfoJson extends DefaultJsonProtocol {
  implicit val developerVersionsInfoJson = jsonFormat1(DeveloperVersionsInfo.apply)
}

case class InstallInfo(account: String, time: Date)

object InstallInfo extends DefaultJsonProtocol {
  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}
