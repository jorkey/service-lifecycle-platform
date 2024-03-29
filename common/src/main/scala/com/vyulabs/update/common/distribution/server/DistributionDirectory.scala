package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory

import java.io._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DistributionDirectory(val directory: File) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val directoryDir = new File(directory, "directory")

  private val developerDir = new File(directoryDir, "developer")
  private val developerServicesDir = new File(developerDir, "services")
  private val developerPrivateDir = new File(developerDir, "private")
  private val developerPrivateCommonDir = new File(developerPrivateDir, "common")
  private val developerPrivateServicesDir = new File(developerPrivateDir, "services")

  private val clientDir = new File(directoryDir, "client")
  private val clientServicesDir = new File(clientDir, "services")
  private val clientPrivateDir = new File(clientDir, "private")
  private val clientPrivateCommonDir = new File(clientPrivateDir, "common")
  private val clientPrivateServicesDir = new File(clientPrivateDir, "services")

  private val faultsDir = new File(directoryDir, "faults")
  private val builderDir = new File(directory, "builder")

  if (!directory.exists()) directory.mkdirs()

  if (!directoryDir.exists()) directoryDir.mkdir()

  if (!developerDir.exists()) developerDir.mkdir()
  if (!developerServicesDir.exists()) developerServicesDir.mkdir()
  if (!developerPrivateDir.exists()) developerPrivateDir.mkdir()
  if (!developerPrivateCommonDir.exists()) developerPrivateCommonDir.mkdir()
  if (!developerPrivateServicesDir.exists()) developerPrivateServicesDir.mkdir()

  if (!clientDir.exists()) clientDir.mkdir()
  if (!clientServicesDir.exists()) clientServicesDir.mkdir()
  if (!clientPrivateDir.exists()) clientPrivateDir.mkdir()
  if (!clientPrivateCommonDir.exists()) clientPrivateCommonDir.mkdir()
  if (!clientPrivateServicesDir.exists()) clientPrivateServicesDir.mkdir()

  if (!faultsDir.exists()) faultsDir.mkdir()
  if (!builderDir.exists()) builderDir.mkdir()

  def getConfigFile(): File = {
    new File(directory, Common.DistributionConfigFileName)
  }

  def getDeveloperVersionImageFileName(service: ServiceId, version: DeveloperVersion): String = {
    service + "-" + version + ".zip"
  }

  def getClientVersionImageFileName(service: ServiceId, version: ClientVersion): String = {
    service + "-" + version + ".zip"
  }

  def getFaultReportFileName(id: String): String = {
    id + "-fault.zip"
  }

  def drop(): Unit = {
    IoUtils.deleteFileRecursively(directory)
  }

  def getDeveloperServiceDir(distribution: DistributionId, service: ServiceId): File = {
    val dir1 = new File(developerServicesDir, distribution)
    if (!dir1.exists()) dir1.mkdir()
    val dir2 = new File(dir1, service)
    if (!dir2.exists()) dir2.mkdir()
    dir2
  }

  def getClientServiceDir(distribution: DistributionId, service: ServiceId): File = {
    val dir1 = new File(clientServicesDir, distribution)
    if (!dir1.exists()) dir1.mkdir()
    val dir2 = new File(dir1, service)
    if (!dir2.exists()) dir2.mkdir()
    dir2
  }

  def getDeveloperVersionImageFile(service: ServiceId, version: DeveloperDistributionVersion): File = {
    new File(getDeveloperServiceDir(version.distribution, service), getDeveloperVersionImageFileName(service, version.developerVersion))
  }

  def getClientVersionImageFile(service: ServiceId, version: ClientDistributionVersion): File = {
    new File(getClientServiceDir(version.distribution, service), getClientVersionImageFileName(service, version.clientVersion))
  }

  def getDeveloperPrivateFile(path: String): File = {
    val index = path.indexOf("/")
    getDeveloperPrivateFile(path.substring(0, index), path.substring(index+1))
  }

  def getDeveloperPrivateFile(service: ServiceId, path: String): File = {
    val file = if (!service.isEmpty) {
      new File(new File(developerPrivateServicesDir, service), path)
    } else {
      new File(developerPrivateCommonDir, path)
    }
    file.getParentFile.mkdirs()
    file
  }

  def getClientPrivateFile(path: String): File = {
    val index = path.indexOf("/")
    getClientPrivateFile(path.substring(0, index), path.substring(index+1))
  }

  def getClientPrivateFile(service: ServiceId, path: String): File = {
    val file = if (!service.isEmpty) {
      new File(new File(clientPrivateServicesDir, service), path)
    } else {
      new File(clientPrivateCommonDir, path)
    }
    file.getParentFile.mkdirs()
    file
  }

  def getFaultsDir(): File = {
    faultsDir
  }

  def getFaultReportFile(id: String): File = {
    new File(faultsDir, getFaultReportFileName(id))
  }

  def getBuilderDir(): File = {
    builderDir
  }

  def getBuilderDir(distribution: DistributionId): File = {
    val dir = new File(builderDir, distribution)
    if (!dir.exists()) dir.mkdir()
    dir
  }
}
