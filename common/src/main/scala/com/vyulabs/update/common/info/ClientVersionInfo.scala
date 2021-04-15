package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion}
import spray.json.DefaultJsonProtocol

case class ClientVersionInfo(service: ServiceId, version: ClientDistributionVersion, buildInfo: BuildInfo, installInfo: InstallInfo)

object ClientVersionInfo extends DefaultJsonProtocol {
  implicit val installVersionInfoJson = jsonFormat4(ClientVersionInfo.apply)

  def from(service: ServiceId, version: ClientDistributionVersion, buildInfo: BuildInfo, installInfo: InstallInfo): ClientVersionInfo = {
    new ClientVersionInfo(service, version, buildInfo, installInfo)
  }
}

case class ClientVersionsInfo(versions: Seq[ClientVersionInfo])

object ClientVersionsInfoJson extends DefaultJsonProtocol {
  implicit val clientVersionsInfoJson = jsonFormat1(ClientVersionsInfo.apply)
}