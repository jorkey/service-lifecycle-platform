package com.vyulabs.libs.git

import com.vyulabs.libs.git.GitRepository.{cloneRepository, openAndPullRepository}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger

import java.io.File
import java.net.URI

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
          if (!IoUtils.deleteFileRecursively(directory)) {
            return None
          }
          cloneRepository(uri, branch, directory, cloneSubmodules)
        case rep =>
          rep
      }
    }
  }
}
