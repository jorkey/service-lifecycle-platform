package com.vyulabs.update.distribution.developer

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
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

  def getDownloadVersionsInfoPath(serviceName: ServiceName, clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        downloadVersionsInfoPath + "/" + serviceName + "/?client=" + clientName
      case None =>
        downloadVersionsInfoPath + "/" + serviceName
    }
  }

  def getDownloadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        downloadDesiredVersionsPath + "/?client=" + clientName
      case None =>
        downloadDesiredVersionsPath
    }
  }

  def getUploadDesiredVersionsPath(clientName: Option[ClientName]): String = {
    clientName match {
      case Some(clientName) =>
        uploadDesiredVersionsPath + "/?client=" + clientName
      case None =>
        uploadDesiredVersionsPath
    }
  }

  def getUploadInstancesStatePath(clientName: ClientName): String = {
    uploadInstancesStatePath + "/" + clientName
  }
}