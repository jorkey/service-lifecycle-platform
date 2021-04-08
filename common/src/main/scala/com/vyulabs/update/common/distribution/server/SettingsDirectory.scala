package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}

import java.io.File

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class SettingsDirectory(builderDirectory: File, distribution: DistributionName) {
  private val settingsDirectory = new File(builderDirectory, s"settings.${distribution}")
  private val servicesDir = new File(settingsDirectory, "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

  settingsDirectory.mkdir()
  servicesDir.mkdir()

  def getSourcesFile(): File = {
    new File(settingsDirectory, "sources.json")
  }

  def getServiceDir(service: ServiceName): File = {
    new File(servicesDir, service)
  }

  def getServiceInstallConfigFile(service: ServiceName): File = {
    new File(getServiceDir(service), Common.InstallConfigFileName)
  }

  def getServiceSettingsDir(service: ServiceName): File = {
    new File(getServiceDir(service), serviceSettingsDirName)
  }

  def getServicePrivateDir(service: ServiceName): File = {
    new File(getServiceDir(service), servicePrivateDirName)
  }
}