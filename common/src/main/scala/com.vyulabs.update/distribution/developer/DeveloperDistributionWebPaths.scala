package com.vyulabs.update.distribution.developer

import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceName}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.distribution.DistributionWebPaths

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait DeveloperDistributionWebPaths extends DistributionWebPaths {
  val uploadTestedVersionsPath = "upload-tested-versions"
  val uploadInstancesStatePath = "upload-instances-state"

  val testedVersionsName = "tested-versions"
  val instancesStateName = "instances-state"
  val serviceFaultName = "service-fault"

  def getDownloadVersionsInfoPath(clientName: Option[ClientName], serviceName: ServiceName): String = {
    downloadVersionsInfoPath + "/" + serviceName + "/" + clientName.getOrElse("")
  }

  def getDownloadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    downloadDesiredVersionsPath + "/" + clientName.getOrElse("")
  }

  def getDownloadDesiredVersionsPath(): String = {
    downloadDesiredVersionsPath
  }

  def getUploadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    uploadDesiredVersionsPath + "/" + clientName.getOrElse("")
  }

  def getUploadInstancesStatePath(clientName: ClientName): String = {
    uploadInstancesStatePath + "/" + clientName
  }
}