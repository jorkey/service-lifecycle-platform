package com.vyulabs.update.distribution.distribution

import com.vyulabs.libs.git.{GitLock, GitRepository}
import com.vyulabs.update.common.Common.ServiceName
import org.slf4j.Logger
import java.io.File
import java.net.URI

import com.vyulabs.update.distribution.{AdminRepository, GitRepositoryUtils}
import com.vyulabs.update.common.Common
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.DeveloperDistributionVersion

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.03.19.
  * Copyright FanDate, Inc.
  */
class ClientAdminRepository(repository: GitRepository)(implicit log: Logger) extends AdminRepository(repository) {
  private val installLogFile = new File(repository.getDirectory(), "install.log")

  private val servicesDir = new File(repository.getDirectory(), "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

  def getInstallLogFile(): File = {
    installLogFile
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

  def processLogFile(completed: Boolean)
                    (implicit log: Logger): Unit = {
    val logFile = getInstallLogFile()
    if (completed) {
      if (logFile.exists()) {
        removeFile(logFile)
      }
    } else {
      if (IoUtils.copyFile(new File("log/installer.log"), logFile)) {
        addFileToCommit(logFile)
      }
    }
  }
}

object ClientAdminRepository {
  def create(directory: File)(implicit log: Logger): Boolean = {
    GitRepository.createBareRepository(directory).map(_ => true).getOrElse(false)
  }

  def apply(uri: URI, directory: File)(implicit log: Logger): Option[ClientAdminRepository] = {
    val rep = GitRepositoryUtils.getGitRepository(uri, "master", false, directory).getOrElse {
      return None
    }
    Some(new ClientAdminRepository(rep))
  }
}