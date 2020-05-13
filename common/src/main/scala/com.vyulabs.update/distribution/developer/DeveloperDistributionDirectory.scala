package com.vyulabs.update.distribution.developer

import java.io.File
import java.nio.channels.FileLock

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, InstallProfileName, ServiceName}
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.{LoggerFactory}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DeveloperDistributionDirectory(directory: File)(implicit filesLocker: SmartFilesLocker)
      extends DistributionDirectory(directory) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val clientsDir = new File(directory, "clients")
  protected val faultsDir = new File(directory, "faults")
  protected val profilesDir = new File(directory, "profiles")

  protected val instancesStateFile = "instances-state.json"
  protected val deadInstancesStateFile = "dead-instances-state.json"

  protected val serviceLogsFile = "%s-logs.json"
  protected val clientLockFile = "client-%s.lock"

  if (!clientsDir.exists()) {
    clientsDir.mkdir()
  }
  if (!profilesDir.exists()) {
    profilesDir.mkdir()
  }
  if (!faultsDir.exists() && !faultsDir.mkdir()) {
    log.error(s"Can't create directory ${faultsDir}")
  }

  def getClientDir(clientName: ClientName): File = {
    val dir = new File(clientsDir, clientName)
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getClientConfigFile(clientName: ClientName): File = {
    new File(getClientDir(clientName), Common.ClientConfigFileName)
  }

  def getServicesDir(clientName: ClientName): File = {
    val dir = new File(getClientDir(clientName), "services")
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getInstallProfileFile(profileName: InstallProfileName): File = {
    new File(profilesDir, Common.InstallProfileFileName.format(profileName))
  }

  def getInstancesStateDir(clientName: ClientName): File = {
    val dir = new File(getClientDir(clientName), "state")
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File = {
    val dir = clientName match {
      case Some(clientName) =>
        new File(getServicesDir(clientName), serviceName)
      case None =>
        new File(servicesDir, serviceName)
    }
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getFaultsDir() = {
    faultsDir
  }

  def getVersionInfoFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName, version.client), getVersionInfoFileName(serviceName, version))
  }

  def getVersionImageFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName, version.client), getVersionImageFileName(serviceName, version))
  }

  def getClientDesiredVersionsFile(clientName: ClientName): File = {
    new File(getClientDir(clientName), desiredVersionsFile)
  }

  def getDesiredVersionsFile(clientName: Option[ClientName]): File = {
    clientName match {
      case Some(clientName) =>
        getClientDesiredVersionsFile(clientName)
      case None =>
        getDesiredVersionsFile()
    }
  }

  def getInstancesStateFile(clientName: ClientName): File = {
    new File(getInstancesStateDir(clientName), instancesStateFile)
  }

  def getDeadInstancesStateFile(clientName: ClientName): File = {
    new File(getInstancesStateDir(clientName), deadInstancesStateFile)
  }

  def getDesiredVersion(serviceName: ServiceName): Option[BuildVersion] = {
    val desiredVersions = Utils.parseConfigFileWithLock(getDesiredVersionsFile(None)).map(DesiredVersions(_))
    desiredVersions match {
      case Some(versions) =>
        versions.Versions.get(serviceName)
      case None =>
        None
    }
  }

  def getDesiredVersions(clientName: Option[ClientName]): Option[DesiredVersions] = {
    Utils.parseConfigFileWithLock(getDesiredVersionsFile(clientName)).map(DesiredVersions(_))
  }
}