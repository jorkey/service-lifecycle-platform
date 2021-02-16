package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}

import java.io.File

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class SettingsDirectory(builderDirectory: File, distributionName: DistributionName) {
  private val settingsDirectory = new File(builderDirectory, s"settings.${distributionName}")
  private val servicesDir = new File(settingsDirectory, "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

  def init(): Unit = {
    settingsDirectory.mkdir()
    servicesDir.mkdir()
  }

  def getSourcesFile(): File = {
    new File("sources.json")
  }

  def getServiceDir(serviceName: ServiceName): File = {
    new File(servicesDir, serviceName)
  }

  def getServiceInstallConfigFile(serviceName: ServiceName): File = {
    new File(getServiceDir(serviceName), Common.InstallConfigFileName)
  }

  def getServiceSettingsDir(serviceName: ServiceName): File = {
    new File(getServiceDir(serviceName), serviceSettingsDirName)
  }

  def getServicePrivateDir(serviceName: ServiceName): File = {
    new File(getServiceDir(serviceName), servicePrivateDirName)
  }
}