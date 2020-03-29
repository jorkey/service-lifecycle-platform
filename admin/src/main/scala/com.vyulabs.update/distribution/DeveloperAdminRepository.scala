package com.vyulabs.update.distribution.distribution

import java.io.File
import java.net.URI

import com.vyulabs.libs.git.{GitLock, GitRepository}
import com.vyulabs.update.distribution.{AdminRepository, GitRepositoryUtils}
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.utils.UpdateUtils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 08.03.19.
  * Copyright FanDate, Inc.
  */
class DeveloperAdminRepository(repository: GitRepository)(implicit log: Logger) extends AdminRepository(repository) {
  private val buildLogFile = new File(repository.getDirectory(), "build.log")

  private val updateSequenceFile = new File(getDirectory(), "prepareUpdate.sequence")

  def getDirectory() = repository.getDirectory()

  def buildVersionLock(serviceName: ServiceName): GitLock = {
    new GitLock(repository, s"lock-build-${serviceName}")
  }

  def buildDesiredVersionsLock(): GitLock = {
    new GitLock(repository, s"lock-desired-versions")
  }

  def getBuildLogFile(): File = {
    buildLogFile
  }

  def readUpdateSequence(): Option[Int] = {
    val updateSequence = if (!updateSequenceFile.exists()) {
      if (!repository.add(updateSequenceFile)) {
        return None
      }
      1
    } else {
      val bytes = UpdateUtils.readFileToBytes(updateSequenceFile).getOrElse {
        return None
      }
      new String(bytes, "utf8").toInt
    }
    Some(updateSequence)
  }

  def writeUpdateSequence(updateSequence: Int): Boolean = {
    if (!UpdateUtils.writeFileFromBytes(updateSequenceFile, updateSequence.toString.getBytes("utf8"))) {
      return false
    }
    repository.add(updateSequenceFile)
  }

  def processLogFile(completed: Boolean)(implicit log: Logger): Unit = {
    val logFile = getBuildLogFile()
    if (completed) {
      if (logFile.exists()) {
        removeFile(logFile)
      }
    } else {
      if (UpdateUtils.copyFile(new File("log/builder.log"), logFile)) {
        addFileToCommit(logFile)
      }
    }
  }
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

  def makeStartOfBuildMessage(author: String, serviceName: ServiceName,
                                      clientName: Option[ClientName], comment: Option[String],
                                      newVersion: Option[BuildVersion]): String = {
    var startMessage = s"Build service ${serviceName}"
    for (newVersion <- newVersion) {
      startMessage += s", version ${newVersion}"
    }
    startMessage += s", author ${author}"
    for (comment <- comment) {
      startMessage += s""", comment "${comment}""""
    }
    startMessage
  }

  def makeEndOfBuildMessage(serviceName: ServiceName,
                                    version: Option[BuildVersion]): String = {
    version match {
      case Some(version) =>
        s"Service ${serviceName} version ${version} is successfully built"
      case None =>
        s"Service ${serviceName} build is failed"
    }
  }
}