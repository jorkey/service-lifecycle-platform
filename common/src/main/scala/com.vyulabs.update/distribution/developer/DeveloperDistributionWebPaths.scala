package com.vyulabs.update.distribution.developer

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.DistributionWebPaths

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait DeveloperDistributionWebPaths extends DistributionWebPaths {
  // New API paths

  val testedVersionsPath = "tested-versions"
  val instancesStatePath = "instances-state"

  val clientConfigPath = "client-config"

  // Old API paths

  val uploadTestedVersionsPath = "upload-tested-versions"
  val uploadInstancesStatePath = "upload-instances-state"

  val downloadClientConfigPath = "download-client-config"

  // Names

  val testedVersionsName = "tested-versions"
  val instancesStateName = "instances-state"
  val serviceFaultName = "service-fault"

  def getDownloadClientConfigPath(): String = {
    downloadClientConfigPath
  }

  def getDownloadVersionsInfoPath(serviceName: ServiceName, clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        downloadVersionsInfoPath + "/" + encode(serviceName) + "?client=" + encode(clientName)
      case None =>
        downloadVersionsInfoPath + "/" + encode(serviceName)
    }
  }

  def getDownloadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        downloadDesiredVersionsPath + "?client=" + encode(clientName)
      case None =>
        downloadDesiredVersionsPath
    }
  }

  def getDownloadDesiredVersionsPath(common: Boolean = false): String = {
    if (!common) {
      downloadDesiredVersionsPath
    } else {
      downloadDesiredVersionsPath + "?common=true"
    }
  }

  def getUploadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        uploadDesiredVersionsPath + "?client=" + encode(clientName)
      case None =>
        uploadDesiredVersionsPath
    }
  }

  def getInstancesStatePath(): String = {
    instancesStatePath
  }
}