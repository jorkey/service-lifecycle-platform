package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.ServiceName
import org.slf4j.Logger

import java.io.File

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class SettingsDirectory(directory: File)(implicit log: Logger) {
  private val servicesDir = new File(directory, "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

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