package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion}
import spray.json.DefaultJsonProtocol

case class ClientVersionInfo(serviceName: ServiceName, distributionName: DistributionName,
                             version: ClientVersion, buildInfo: BuildInfo, installInfo: InstallInfo)

object ClientVersionInfo extends DefaultJsonProtocol {
  implicit val installVersionInfoJson = jsonFormat5(ClientVersionInfo.apply)

  def apply(serviceName: ServiceName, version: ClientDistributionVersion, buildInfo: BuildInfo, installInfo: InstallInfo): ClientVersionInfo = {
    new ClientVersionInfo(serviceName, version.distributionName, version.version, buildInfo, installInfo)
  }
}

case class ClientVersionsInfo(versions: Seq[ClientVersionInfo])

object ClientVersionsInfoJson extends DefaultJsonProtocol {
  implicit val clientVersionsInfoJson = jsonFormat1(ClientVersionsInfo.apply)
}