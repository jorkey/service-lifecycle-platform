package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServiceName}
import com.vyulabs.update.common.version.{ClientDistributionVersion}
import spray.json.DefaultJsonProtocol

case class ClientVersionInfo(serviceName: ServiceName, version: ClientDistributionVersion, buildInfo: BuildInfo, installInfo: InstallInfo)

object ClientVersionInfo extends DefaultJsonProtocol {
  implicit val installVersionInfoJson = jsonFormat4(ClientVersionInfo.apply)
}

case class ClientVersionsInfo(versions: Seq[ClientVersionInfo])

object ClientVersionsInfoJson extends DefaultJsonProtocol {
  implicit val clientVersionsInfoJson = jsonFormat1(ClientVersionsInfo.apply)
}