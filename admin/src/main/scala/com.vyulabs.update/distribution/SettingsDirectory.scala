package com.vyulabs.update.distribution

import com.vyulabs.libs.git.GitRepository

import java.io.File
import java.net.URI
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import org.slf4j.Logger

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

object SettingsDirectory {
  def create(directory: File)(implicit log: Logger): Boolean = {
    GitRepository.createBareRepository(directory).map(_ => true).getOrElse(false)
  }

  def apply(uri: URI, directory: File)(implicit log: Logger): Option[SettingsDirectory] = {
    val rep = GitRepositoryUtils.getGitRepository(uri, "master", false, directory).getOrElse {
      return None
    }
    Some(new SettingsDirectory(rep.getDirectory()))
  }

  def apply(directory: File)(implicit log: Logger): SettingsDirectory = {
    new SettingsDirectory(directory)
  }
}