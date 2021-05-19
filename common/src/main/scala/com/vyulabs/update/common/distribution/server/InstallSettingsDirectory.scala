package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}

import java.io.File

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class InstallSettingsDirectory(builderDirectory: File, distribution: DistributionId) {
  private val directory = new File(builderDirectory, s"install.${distribution}")
  private val servicesDir = new File(directory, "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

  directory.mkdir()
  servicesDir.mkdir()

  def getServiceDir(service: ServiceId): File = {
    new File(servicesDir, service)
  }

  def getServiceInstallConfigFile(service: ServiceId): File = {
    new File(getServiceDir(service), Common.InstallConfigFileName)
  }

  def getServiceSettingsDir(service: ServiceId): File = {
    new File(getServiceDir(service), serviceSettingsDirName)
  }

  def getServicePrivateDir(service: ServiceId): File = {
    new File(getServiceDir(service), servicePrivateDirName)
  }
}