package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class InstalledVersionInfo(serviceName: ServiceName, version: BuildVersion, buildInfo: BuildInfo, installInfo: InstallInfo)

object InstalledVersionInfo extends DefaultJsonProtocol {
  implicit val installVersionInfoJson = jsonFormat4(InstalledVersionInfo.apply)
}

case class InstalledVersionsInfo(versions: Seq[InstalledVersionInfo])

object InstalledVersionsInfoJson extends DefaultJsonProtocol {
  implicit val installVersionsInfoJson = jsonFormat1(InstalledVersionsInfo.apply)
}