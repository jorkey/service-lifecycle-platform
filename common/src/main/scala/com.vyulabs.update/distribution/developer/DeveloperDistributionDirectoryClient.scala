package com.vyulabs.update.distribution.developer

import java.io._
import java.net.URL

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceName}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.state.InstancesState
import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DeveloperDistributionDirectoryClient(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with DeveloperDistributionWebPaths {

  def downloadVersionsInfo(clientName: Option[ClientName], serviceName: ServiceName): Option[VersionsInfo] = {
    val url = makeUrl(getDownloadVersionsInfoPath(clientName, serviceName))
    downloadToConfig(url) match {
      case Some(config) =>
        Some(VersionsInfo(config))
      case None =>
        None
    }
  }

  def downloadDesiredVersionsConfig(clientName: Option[ClientName]): Option[Config] = {
    clientName match {
      case Some(clientName) =>
        log.info(s"Download desired versions for client ${clientName}")
      case None =>
        log.info(s"Download desired versions")
    }
    val url = makeUrl(getDownloadDesiredVersionsPath(clientName))
    if (exists(url)) {
      downloadToConfig(url) match {
        case Some(config) =>
          Some(config)
        case None =>
          None
      }
    } else {
      None
    }
  }

  def downloadDesiredVersions(clientName: Option[ClientName]): Option[DesiredVersions] = {
    downloadDesiredVersionsConfig(clientName).map(DesiredVersions(_))
  }

  def downloadDesiredVersions(clientName: ClientName): Option[DesiredVersions] = {
    val commonConfig = downloadDesiredVersionsConfig(None)
    val clientConfig = downloadDesiredVersionsConfig(Some(clientName))
    val mergedConfig = (commonConfig, clientConfig) match {
      case (Some(commonConfig), Some(clientConfig)) =>
        Some(clientConfig.withFallback(commonConfig))
      case (Some(commonConfig), None) =>
        Some(commonConfig)
      case (None, Some(clientConfig)) =>
        Some(clientConfig)
      case (None, None) =>
        None
    }
    mergedConfig.map(DesiredVersions(_))
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

  def uploadTestedVersions(testedVersions: ServicesVersions): Boolean = {
    log.info(s"Upload tested versions")
    uploadFromConfig(makeUrl(uploadTestedVersionsPath),
      testedVersionsName, uploadTestedVersionsPath, testedVersions.toConfig())
  }

  def uploadInstancesState(clientName: ClientName, instancesState: InstancesState): Boolean = {
    uploadFromConfig(makeUrl(getUploadInstancesStatePath(clientName)),
      instancesStateName, uploadInstancesStatePath, instancesState.toConfig())
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
