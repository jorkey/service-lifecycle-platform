package com.vyulabs.update.distribution.server

import java.io._

import com.vyulabs.update.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
class DistributionDirectory(val directory: File) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected val developerDir = new File(directory, "developer")
  protected val developerServicesDir = new File(developerDir, "services")
  protected val clientDir = new File(directory, "client")
  protected val clientServicesDir = new File(clientDir, "services")
  protected val faultsDir = new File(directory, "faults")

  if (!directory.exists()) directory.mkdirs()
  if (!developerDir.exists()) developerDir.mkdir()
  if (!developerServicesDir.exists()) developerServicesDir.mkdir()
  if (!clientDir.exists()) clientDir.mkdir()
  if (!clientServicesDir.exists()) clientServicesDir.mkdir()
  if (!faultsDir.exists()) faultsDir.mkdir()

  def getDeveloperVersionImageFileName(serviceName: ServiceName, version: DeveloperVersion): String = {
    serviceName + "-" + version + ".zip"
  }

  def getClientVersionImageFileName(serviceName: ServiceName, version: ClientVersion): String = {
    serviceName + "-" + version + ".zip"
  }

  def getFaultReportFileName(faultId: String): String = {
    faultId + "-fault.zip"
  }

  def drop(): Unit = {
    IoUtils.deleteFileRecursively(directory)
  }

  def getDeveloperServiceDir(distributionName: DistributionName, serviceName: ServiceName): File = {
    val dir1 = new File(developerServicesDir, distributionName)
    if (!dir1.exists()) dir1.mkdir()
    val dir2 = new File(dir1, serviceName)
    if (!dir2.exists()) dir2.mkdir()
    dir2
  }

  def getClientServiceDir(distributionName: DistributionName, serviceName: ServiceName): File = {
    val dir1 = new File(clientServicesDir, distributionName)
    if (!dir1.exists()) dir1.mkdir()
    val dir2 = new File(dir1, serviceName)
    if (!dir2.exists()) dir2.mkdir()
    dir2
  }

  def getDeveloperVersionImageFile(serviceName: ServiceName, version: DeveloperDistributionVersion): File = {
    new File(getDeveloperServiceDir(version.distributionName, serviceName), getDeveloperVersionImageFileName(serviceName, version.version))
  }

  def getClientVersionImageFile(serviceName: ServiceName, version: ClientDistributionVersion): File = {
    new File(getClientServiceDir(version.distributionName, serviceName), getClientVersionImageFileName(serviceName, version.version))
  }

  def getFaultsDir(): File = {
    faultsDir
  }

  def getFaultReportFile(faultId: String): File = {
    new File(faultsDir, getFaultReportFileName(faultId))
  }
}
