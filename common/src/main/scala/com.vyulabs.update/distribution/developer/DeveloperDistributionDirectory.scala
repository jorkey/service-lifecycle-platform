package com.vyulabs.update.distribution.developer

import java.io.File
import java.util.regex.{MatchResult, Pattern}

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.info.{DesiredVersions}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.IoUtils
import org.slf4j.LoggerFactory

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

  protected val testedVersionsFile = "tested-versions-%s.json"

  protected val instancesStateFile = "instances-state.json"
  protected val deadInstancesStateFile = "dead-instances-state.json"

  protected val serviceLogsFile = "%s-logs.json"
  protected val clientLockFile = "client-%s.lock"

  protected val installedDesiredVersionsFile = "installed-desired-versions.json"

  if (!clientsDir.exists()) {
    clientsDir.mkdir()
  }
  if (!profilesDir.exists()) {
    profilesDir.mkdir()
  }
  if (!faultsDir.exists() && !faultsDir.mkdir()) {
    log.error(s"Can't create directory ${faultsDir}")
  }

  override def getServiceDir(serviceName: ServiceName, clientName: Option[ClientName]): File = {
    clientName match {
      case Some(clientName) =>
        val dir = new File(getServicesDir(clientName), serviceName)
        if (!dir.exists()) dir.mkdir()
        dir
      case None =>
        getServiceDir(serviceName)
    }
  }

  def getClientsDir() = clientsDir

  def getClients(): Set[ClientName] = {
    getClientsDir().list().toSet
  }

  def getClientDir(clientName: ClientName): File = {
    new File(clientsDir, clientName)
  }

  def getClientConfigFile(clientName: ClientName): File = {
    new File(getClientDir(clientName), Common.ClientConfigFileName)
  }

  def getServicesDir(clientName: ClientName): File = {
    val dir = new File(getClientDir(clientName), "services")
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getProfiles(): Set[ProfileName] = {
    profilesDir.list().map { name =>
      val pattern = Pattern.compile(Common.ProfileFileNamePattern)
      val matcher = pattern.matcher(name)
      if (matcher.find()) {
        Some(matcher.group(2))
      } else {
        None
      }
    }.flatten.toSet
  }

  def getProfileFile(profileName: ProfileName): File = {
    val pattern = Pattern.compile(Common.ProfileFileNamePattern)
    val matcher = pattern.matcher(Common.ProfileFileNameMatch)
    val fileName = matcher.replaceAll((r: MatchResult) => { r.group(1) + profileName + r.group(3) })
    new File(profilesDir, fileName)
  }

  def getInstancesStateDir(clientName: ClientName): File = {
    val dir = new File(getClientDir(clientName), "state")
    if (!dir.exists()) dir.mkdir()
    dir
  }

  def getFaultsDir() = {
    faultsDir
  }

  def getDesiredVersionsFile(clientName: ClientName): File = {
    new File(getClientDir(clientName), desiredVersionsFile)
  }

  def getInstalledDesiredVersionsFile(clientName: ClientName): File = {
    new File(getClientDir(clientName), installedDesiredVersionsFile)
  }

  def getDesiredVersionsFile(clientName: Option[ClientName]): File = {
    clientName match {
      case Some(clientName) =>
        getDesiredVersionsFile(clientName)
      case None =>
        getDesiredVersionsFile()
    }
  }

  def getTestedVersionsFile(profileName: ProfileName): File = {
    new File(directory, testedVersionsFile.format(profileName))
  }

  def getInstancesStateFile(clientName: ClientName): File = {
    new File(getInstancesStateDir(clientName), instancesStateFile)
  }

  def getDeadInstancesStateFile(clientName: ClientName): File = {
    new File(getInstancesStateDir(clientName), deadInstancesStateFile)
  }

  def getDesiredVersions(clientName: Option[ClientName]): Option[DesiredVersions] = {
    IoUtils.readFileToJson[DesiredVersions](getDesiredVersionsFile(clientName))
  }
}