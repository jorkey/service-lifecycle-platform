package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.{ClientDistributionVersion}
import spray.json.DefaultJsonProtocol

case class ClientVersionInfo(serviceName: ServiceName, version: ClientDistributionVersion, buildInfo: BuildInfo, installInfo: InstallInfo)

object ClientVersionInfo extends DefaultJsonProtocol {
  implicit val installVersionInfoJson = jsonFormat4(ClientVersionInfo.apply)
}

case class ClientVersionsInfo(versions: Seq[ClientVersionInfo])

object ClientVersionsInfoJson extends DefaultJsonProtocol {
  implicit val clientVersionsInfoJson = jsonFormat1(ClientVersionsInfo.apply)
}