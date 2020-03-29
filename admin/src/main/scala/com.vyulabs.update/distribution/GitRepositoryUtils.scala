package com.vyulabs.update.distribution

import java.io.File
import java.net.URI

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.libs.git.GitRepository.{cloneRepository, openAndPullRepository}
import com.vyulabs.update.utils.UpdateUtils
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object GitRepositoryUtils {
  def getGitRepository(uri: URI, branch: String, cloneSubmodules: Boolean, directory: File)(implicit log: Logger): Option[GitRepository] = {
    if (!directory.exists()) {
      cloneRepository(uri, branch, directory, cloneSubmodules)
    } else {
      openAndPullRepository(uri, branch, directory) match {
        case None =>
          if (!UpdateUtils.deleteFileRecursively(directory)) {
            return None
          }
          cloneRepository(uri, branch, directory, cloneSubmodules)
        case rep =>
          return rep
      }
    }
  }
}
