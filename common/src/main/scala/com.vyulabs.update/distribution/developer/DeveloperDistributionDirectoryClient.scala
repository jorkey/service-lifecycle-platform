package com.vyulabs.update.distribution.developer

import java.io._
import java.net.URL

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.config.ClientConfig
import com.vyulabs.update.info.{DesiredVersions, InstancesState, ServicesVersions, TestedVersions, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import org.slf4j.Logger
import com.vyulabs.update.config.ClientConfig._
import com.vyulabs.update.info.DesiredVersions._
import com.vyulabs.update.info.ServicesVersions._
import InstancesState._
import spray.json._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DeveloperDistributionDirectoryClient(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with DeveloperDistributionWebPaths {

  def downloadClientConfig(): Option[ClientConfig] = {
    log.info(s"Download client config")
    val url = makeUrl(getDownloadClientConfigPath())
    downloadToJson(url).map(_.convertTo[ClientConfig])
  }

  def downloadDesiredVersions(common: Boolean = false): Option[DesiredVersions] = {
    log.info(s"Download desired versions")
    val url = makeUrl(getDownloadDesiredVersionsPath(common))
    downloadToJson(url).map(_.convertTo[DesiredVersions])
  }

  def downloadTestedVersions(): Option[TestedVersions] = {
    log.info(s"Download tested versions")
    val url = makeUrl(apiPathPrefix + "/" + testedVersionsPath)
    downloadToJson(url).map(_.convertTo[TestedVersions])
  }

  def uploadInstalledDesiredVersions(file: File): Boolean = {
    log.info(s"Upload installed desired versions")
    uploadFromFile(makeUrl(apiPathPrefix + "/" + installedDesiredVersionsPath), desiredVersionsName, file)
  }

  def uploadTestedVersions(testedVersions: ServicesVersions): Boolean = {
    log.info(s"Upload tested versions")
    uploadFromJson(makeUrl(apiPathPrefix + "/" + testedVersionsPath),
      testedVersionsName, uploadTestedVersionsPath, testedVersions.toJson)
  }

  def uploadInstancesState(instancesState: InstancesState): Boolean = {
    uploadFromJson(makeUrl(apiPathPrefix + "/" + getInstancesStatePath()),
      instancesStateName, instancesStatePath, instancesState.toJson)
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
