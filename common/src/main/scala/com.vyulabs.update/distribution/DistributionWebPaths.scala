package com.vyulabs.update.distribution

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait DistributionWebPaths {
  val downloadVersionPath = "download-version"
  val downloadVersionInfoPath = "download-version-info"
  val downloadVersionsInfoPath = "download-versions-info"
  val downloadDesiredVersionsPath = "download-desired-versions"
  val downloadDesiredVersionPath = "download-desired-version"

  val versionName = "version"
  val versionInfoName = "version-info"
  val desiredVersionsName = "desired-versions"

  val uploadVersionPath = "upload-version"
  val uploadVersionInfoPath = "upload-version-info"
  val uploadDesiredVersionsPath = "upload-desired-versions"
  val uploadServiceFaultPath = "upload-service-fault"

  val getDistributionVersionPath = "get-distribution-version"
  val getScriptsVersionPath = "get-scripts-version"

  val browsePath = "browse"
  val pingPath = "ping"

  def getDownloadVersionPath(serviceName: ServiceName, version: BuildVersion): String = {
    downloadVersionPath + "/" + serviceName + "/" + version.toString
  }

  def getDownloadVersionInfoPath(serviceName: ServiceName, version: BuildVersion): String = {
    downloadVersionInfoPath + "/" + serviceName + "/" + version.toString
  }

  def getUploadVersionPath(serviceName: ServiceName, version: BuildVersion): String = {
    uploadVersionPath + "/" + serviceName + "/" + version.toString
  }

  def getUploadVersionInfoPath(serviceName: ServiceName, version: BuildVersion): String = {
    uploadVersionInfoPath + "/" + serviceName + "/" + version.toString
  }

  def getUploadServiceFaultPath(serviceName: ServiceName): String = {
    uploadServiceFaultPath + "/" + serviceName.toString
  }
}
