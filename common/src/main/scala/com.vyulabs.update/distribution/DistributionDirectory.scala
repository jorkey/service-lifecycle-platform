package com.vyulabs.update.distribution

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.info.{DesiredVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.BuildVersion

import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DistributionDirectory(val directory: File)(implicit filesLocker: SmartFilesLocker) {
  protected val servicesDir = new File(directory, "services")

  protected val desiredVersionsFile = "desired-versions.json"

  if (!directory.exists()) {
    directory.mkdirs()
  }
  if (!servicesDir.exists()) {
    servicesDir.mkdir()
  }

  def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File = {
    getServiceDir(serviceName)
  }

  def getServiceDir(serviceName: ServiceName): File = {
    val dir = new File(servicesDir, serviceName)
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getVersionInfoFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName, version.client), getVersionInfoFileName(serviceName, version))
  }

  def getVersionImageFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName, version.client), getVersionImageFileName(serviceName, version))
  }

  def getDesiredVersion(serviceName: ServiceName)(implicit log: Logger): Option[BuildVersion] = {
    getDesiredVersions() match {
      case Some(versions) =>
        versions.desiredVersions.get(serviceName)
      case None =>
        None
    }
  }

  def getDesiredVersions()(implicit log: Logger): Option[DesiredVersions] = {
    IoUtils.readFileToJson(getDesiredVersionsFile()).map(_.convertTo[DesiredVersions])
  }

  def getDesiredVersionsFile(): File = {
    new File(directory, desiredVersionsFile)
  }

  def getVersionInfoFileName(serviceName: ServiceName, version: BuildVersion): String = {
    serviceName + "-" + version + "-info.json"
  }

  def getVersionImageFileName(serviceName: ServiceName, version: BuildVersion): String = {
    serviceName + "-" + version + ".zip"
  }

  def removeVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    getVersionImageFile(serviceName, version).delete()
    getVersionInfoFile(serviceName, version).delete()
  }
}
