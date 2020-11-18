package com.vyulabs.update.distribution

import java.io.File
import java.net.URI

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.DeveloperDistributionVersion
import org.eclipse.jgit.transport.RefSpec
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.01.20.
  * Copyright FanDate, Inc.
  */
class AdminRepository(repository: GitRepository)(implicit log: Logger) {
  private val desiredVersionsFile = new File(repository.getDirectory(), "desired-versions.json")
  private val servicesDir = new File(repository.getDirectory(), "services")

  private val serviceSettingsDirName = "settings"
  private val servicePrivateDirName = "private"

  def getDirectory() = repository.getDirectory()

  def getDesiredVersionsFile(): File = {
    desiredVersionsFile
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

  def tagServices(serviceNames: Seq[ServiceName]): Boolean = {
    serviceNames.foreach(serviceName =>
      if (!repository.setTag(serviceName, None, true)) {
        return false
      })
    repository.push(serviceNames.map(new RefSpec(_)), false, true)
  }

  def addFileToCommit(file: File): Boolean = {
    repository.add(file)
  }

  def removeFile(file: File): Boolean = {
    repository.remove(file)
  }

  def pull(): Boolean = {
    repository.pull()
  }
}

object AdminRepository {
  def create(directory: File)(implicit log: Logger): Boolean = {
    GitRepository.createBareRepository(directory).map(_ => true).getOrElse(false)
  }

  def apply(uri: URI, directory: File)(implicit log: Logger): Option[AdminRepository] = {
    val rep = GitRepositoryUtils.getGitRepository(uri, "master", false, directory).getOrElse {
      return None
    }
    Some(new AdminRepository(rep))
  }

  def makeStartOfSettingDesiredVersionsMessage(versions: Map[ServiceName, Option[DeveloperDistributionVersion]]): String = {
    if (versions.size == 1) {
      s"Set desired version " + makeDesiredVersionsStr(versions)
    } else {
      s"Set desired versions " + makeDesiredVersionsStr(versions)
    }
  }

  def makeEndOfSettingDesiredVersionsMessage(completed: Boolean): String = {
    if (completed) {
      "Desired versions are successfully assigned"
    } else {
      "Desired versions assign is failed"
    }
  }

  def makeStartOfSettingTestedFlagMessage(): String = {
    "Start marking of desired versions as tested"
  }

  def makeContinueOfSettingTestedFlagMessage(): String = {
    "Continue marking of desired versions as tested"
  }

  def makeStopOfSettingTestedFlagMessage(): String = {
    "Stop marking of desired versions as tested"
  }

  def makeStartOfRemovingTestedFlagMessage(): String = {
    "Start marking of desired versions as tested"
  }

  def makeContinueOfRemovingTestedFlagMessage(): String = {
    "Continue marking of desired versions as tested"
  }

  def makeStopOfRemovingTestedFlagMessage(): String = {
    "Stop marking of desired versions as tested"
  }

  private def makeDesiredVersionsStr(versions: Map[ServiceName, Option[DeveloperDistributionVersion]]): String = {
    versions.foldLeft("") {
      case (versions, record) =>
        val str = record match {
          case (service, version) =>
            service + "->" + version.getOrElse("-")
        }
        versions + (if (versions.isEmpty) str else ", " + str)
    }
  }
}