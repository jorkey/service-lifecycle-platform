package com.vyulabs.update.distribution.client

import java.io.File

import com.vyulabs.update.common.Common.{ClientName, InstanceId, ProcessId, ServiceName}
import com.vyulabs.update.info.DesiredVersions
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.UpdateUtils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class ClientDistributionDirectory(directory: File)(implicit filesLocker: SmartFilesLocker)
      extends DistributionDirectory(directory) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val statesDir = new File(directory, "states")
  protected val logsDir = new File(directory, "logs")
  protected val faultsDir = new File(directory, "faults")

  if (!statesDir.exists() && !statesDir.mkdir()) {
    log.error(s"Can't create directory ${statesDir}")
  }

  if (!logsDir.exists() && !logsDir.mkdir()) {
    log.error(s"Can't create directory ${logsDir}")
  }

  if (!faultsDir.exists() && !faultsDir.mkdir()) {
    log.error(s"Can't create directory ${faultsDir}")
  }

  def getStatesDir() = statesDir

  def getLogsDir() = logsDir

  def getFaultsDir() = faultsDir

  def getInstanceStateFileName(instanceId: InstanceId, updaterProcessId: ProcessId): String = {
    s"${instanceId}_${updaterProcessId}_state.json"
  }

  def getServiceDir(serviceName: ServiceName): File = {
    val dir = new File(servicesDir, serviceName)
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File = {
    val dir = getServiceDir(serviceName)
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getVersionInfoFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName), getVersionInfoFileName(serviceName, version))
  }

  def getVersionImageFile(serviceName: ServiceName, version: BuildVersion): File = {
    new File(getServiceDir(serviceName), getVersionImageFileName(serviceName, version))
  }

  def getInstanceStateFile(instanceId: InstanceId, updaterProcessId: ProcessId): File = {
    new File(statesDir, getInstanceStateFileName(instanceId, updaterProcessId))
  }

  def getDesiredVersion(serviceName: ServiceName): Option[BuildVersion] = {
    getDesiredVersions() match {
      case Some(versions) =>
        versions.Versions.get(serviceName)
      case None =>
        None
    }
  }

  def getDesiredVersions(): Option[DesiredVersions] = {
    val config = UpdateUtils.parseConfigFileWithLock(getDesiredVersionsFile())
    config.map(DesiredVersions(_))
  }
}