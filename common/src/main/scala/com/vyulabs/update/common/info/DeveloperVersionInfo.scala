package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServiceId, TaskId, UserId}
import com.vyulabs.update.common.config.SourceConfig
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol

import java.util.Date

case class BuildInfo(author: String, sources: Seq[SourceConfig], date: Date, comment: Option[String])

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

case class InstallInfo(user: String, date: Date)

object InstallInfo extends DefaultJsonProtocol {
  implicit val installInfoJson = jsonFormat2(InstallInfo.apply)
}

case class DeveloperVersionInProcessInfo(service: ServiceId, version: DeveloperVersion, author: UserId,
                                         sources: Seq[SourceConfig], comment: Option[String],
                                         taskId: TaskId, startTime: Date)
