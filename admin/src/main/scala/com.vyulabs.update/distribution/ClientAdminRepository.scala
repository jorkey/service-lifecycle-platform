package com.vyulabs.update.distribution.distribution

import com.vyulabs.libs.git.{GitRepository}
import com.vyulabs.update.common.Common.ServiceName
import org.slf4j.Logger
import java.io.File
import java.net.URI

import com.vyulabs.update.distribution.{AdminRepository, GitRepositoryUtils}
import com.vyulabs.update.common.Common

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.03.19.
  * Copyright FanDate, Inc.
  */
class ClientAdminRepository(repository: GitRepository)(implicit log: Logger) extends AdminRepository(repository) {
  private val servicesDir = new File(repository.getDirectory(), "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

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