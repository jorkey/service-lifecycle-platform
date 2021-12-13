package com.vyulabs.update.common.distribution.server

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}

import java.io.File

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class ServiceSettingsDirectory(directory: File) {
  private val configDirName = "config"
  private val privateDirName = "private"

  def getInstallConfigFile(): File = {
    new File(directory, Common.InstallConfigFileName)
  }

  def getConfigDir(): File = {
    new File(directory, configDirName)
  }

  def getPrivateDir(): File = {
    new File(directory, privateDirName)
  }
}