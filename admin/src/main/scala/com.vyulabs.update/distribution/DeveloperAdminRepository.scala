package com.vyulabs.update.distribution

import java.io.File
import java.net.URI

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.distribution.ClientAdminRepository
import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 19.03.19.
  * Copyright FanDate, Inc.
  */
class DeveloperAdminRepository(repository: GitRepository)(implicit log: Logger) extends AdminRepository(repository) {
  def getDirectory() = repository.getDirectory()
}

object DeveloperAdminRepository {
  def create(directory: File)(implicit log: Logger): Boolean = {
    GitRepository.createBareRepository(directory).map(_ => true).getOrElse(false)
  }

  def apply(uri: URI, directory: File)(implicit log: Logger): Option[DeveloperAdminRepository] = {
    val rep = GitRepositoryUtils.getGitRepository(uri, "master", false, directory).getOrElse {
      return None
    }
    Some(new DeveloperAdminRepository(rep))
  }
}