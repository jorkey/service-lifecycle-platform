package com.vyulabs.update.distribution

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.info.{VersionInfo, VersionsInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IOUtils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import com.vyulabs.update.info.VersionInfoJson._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
abstract class DistributionDirectory(val directory: File)(implicit filesLocker: SmartFilesLocker) {
  protected val servicesDir = new File(directory, "services")

  protected val desiredVersionsFile = "desired-versions.json"

  if (!directory.exists()) {
    directory.mkdirs()
  }
  if (!servicesDir.exists()) {
    servicesDir.mkdir()
  }

  def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File

  def getVersionInfoFile(serviceName: ServiceName, version: BuildVersion): File

  def getVersionImageFile(serviceName: ServiceName, version: BuildVersion): File

  def getDesiredVersionsFile(): File = {
    new File(directory, desiredVersionsFile)
  }

  def getVersionInfoFileName(serviceName: ServiceName, version: BuildVersion): String = {
    serviceName + "-" + version + "-info.json"
  }

  def getVersionImageFileName(serviceName: ServiceName, version: BuildVersion): String = {
    serviceName + "-" + version + ".zip"
  }

  def getVersionsInfo(directory: File)(implicit log: Logger): VersionsInfo = {
    var versions = Seq.empty[VersionInfo]
    if (directory.exists()) {
      for (file <- directory.listFiles()) {
        if (file.getName.endsWith("-info.json")) {
          try {
            versions ++= IOUtils.readFileToJson(file).map(_.convertTo[VersionInfo])
          } catch {
            case e: Exception =>
              log.error(s"Parse file ${file} error", e)
          }
        }
      }
    }
    VersionsInfo(versions)
  }

  def removeVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    getVersionImageFile(serviceName, version).delete()
    getVersionInfoFile(serviceName, version).delete()
  }
}
