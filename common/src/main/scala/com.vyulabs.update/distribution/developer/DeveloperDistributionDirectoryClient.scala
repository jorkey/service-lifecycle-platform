package com.vyulabs.update.distribution.developer

import java.io._
import java.net.URL

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceName}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.config.ClientConfig
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

  def downloadClientConfig(): Option[ClientConfig] = {

  }

  def downloadDesiredVersions(common: Boolean = false): Option[DesiredVersions] = {
    log.info(s"Download desired versions")
    val url = makeUrl(getDownloadDesiredVersionsPath(common))
    downloadToConfig(url) match {
      case Some(config) =>
        Some(DesiredVersions(config))
      case None =>
        None
    }
  }

  def uploadTestedVersions(testedVersions: ServicesVersions): Boolean = {
    log.info(s"Upload tested versions")
    uploadFromConfig(makeUrl(uploadTestedVersionsPath),
      testedVersionsName, uploadTestedVersionsPath, testedVersions.toConfig())
  }

  def uploadInstancesState(instancesState: InstancesState): Boolean = {
    uploadFromConfig(makeUrl(getUploadInstancesStatePath()),
      instancesStateName, uploadInstancesStatePath, instancesState.toConfig())
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
