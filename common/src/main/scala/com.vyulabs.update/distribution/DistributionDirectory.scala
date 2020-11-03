package com.vyulabs.update.distribution

import java.io._

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DistributionDirectory(val directory: File)(implicit filesLocker: SmartFilesLocker) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val servicesDir = new File(directory, "services")

  protected val desiredVersionsFile = "desired-versions.json"
  protected val logsDir = new File(directory, "logs")
  protected val faultsDir = new File(directory, "faults")

  if (!logsDir.exists() && !logsDir.mkdir()) {
    log.error(s"Can't create directory ${logsDir}")
  }

  if (!faultsDir.exists() && !faultsDir.mkdir()) {
    log.error(s"Can't create directory ${faultsDir}")
  }

  def getLogsDir() = logsDir

  def getFaultsDir() = faultsDir

  if (!directory.exists()) {
    directory.mkdirs()
  }
  if (!servicesDir.exists()) {
    servicesDir.mkdir()
  }

  def drop(): Unit = {
    IoUtils.deleteFileRecursively(directory)
  }

  def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File = {
    getServiceDir(serviceName)
  }

  def getServiceDir(serviceName: ServiceName): File = {
    val dir = new File(servicesDir, serviceName)
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getVersionImageFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName, version.client), getVersionImageFileName(serviceName, version))
  }

  def getVersionImageFileName(serviceName: ServiceName, version: BuildVersion): String = {
    serviceName + "-" + version + ".zip"
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    getVersionImageFile(serviceName, version).delete()
  }

  def removeClientVersion(serviceName: ServiceName, version: BuildVersion): Unit = {
    // TODO graphql
  }
}
