package com.vyulabs.update.distribution.developer

import java.net.URL

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, VersionsInfo}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DeveloperDistributionDirectoryAdmin(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with DeveloperDistributionWebPaths {

  def downloadVersionsInfo(clientName: Option[ClientName], serviceName: ServiceName): Option[VersionsInfo] = {
    val url = makeUrl(getDownloadVersionsInfoPath(serviceName, clientName))
    downloadToConfig(url) match {
      case Some(config) =>
        Some(VersionsInfo(config))
      case None =>
        None
    }
  }

  def downloadDesiredVersions(clientName: Option[ClientName]): Option[DesiredVersions] = {
    clientName match {
      case Some(clientName) =>
        log.info(s"Download desired versions for client ${clientName}")
      case None =>
        log.info(s"Download desired versions")
    }
    val url = makeUrl(getDownloadDesiredVersionsPath(clientName))
    downloadToConfig(url) match {
      case Some(config) =>
        Some(DesiredVersions(config))
      case None =>
        None
    }
  }

  def uploadDesiredVersions(clientName: Option[ClientName], desiredVersions: DesiredVersions): Boolean = {
    clientName match {
      case Some(clientName) =>
        log.info(s"Upload desired versions for client ${clientName}")
      case None =>
        log.info(s"Upload desired versions")
    }
    uploadFromConfig(makeUrl(getUploadDesiredVersionsPath(clientName)),
      desiredVersionsName, uploadDesiredVersionsPath, desiredVersions.toConfig())
  }
}
