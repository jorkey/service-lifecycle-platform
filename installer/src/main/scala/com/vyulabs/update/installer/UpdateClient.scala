package com.vyulabs.update.installer

import java.io.File
import java.util.Date

import com.vyulabs.update.distribution.distribution.ClientAdminRepository
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.AdminRepository
import com.vyulabs.update.info.{DesiredVersions, ServicesVersions, BuildVersionInfo}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectoryClient
import com.vyulabs.update.settings.{ConfigSettings, DefinesSettings}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.enrichAny
import com.vyulabs.update.info.DesiredVersions._
import com.vyulabs.update.installer.InstallResult.InstallResult

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class UpdateClient()(implicit log: Logger) {
  private val buildDir = new File("build")
  private val indexPattern = "(.*)\\.([0-9]*)".r

  def installUpdates(adminRepository: ClientAdminRepository,
                     clientDistribution: ClientDistributionDirectoryClient,
                     developerDistribution: DeveloperDistributionDirectoryClient,
                     servicesOnly: Option[Set[ServiceName]],
                     localConfigOnly: Boolean,
                     assignDesiredVersions: Boolean): InstallResult = {
    val gitLock = adminRepository.buildUpdateLock()
    if (gitLock.lock(ClientAdminRepository.makeStartOfUpdatesMessage(servicesOnly, localConfigOnly, assignDesiredVersions),
        s"Continue install of updates")) {
      var success = false
      var clientVersions = Map.empty[ServiceName, BuildVersion]
      try {
        if (buildDir.exists() && !IoUtils.deleteFileRecursively(buildDir)) {
          log.error(s"Can't remove directory ${buildDir}")
          return InstallResult.Failure
        }
        if (!buildDir.mkdir()) {
          log.error(s"Can't make directory ${buildDir}")
          return InstallResult.Failure
        }
        log.info("Get client config")
        val clientConfig = developerDistribution.downloadClientConfig().getOrElse {
          log.error(s"Can't get client config")
          return InstallResult.Failure
        }
        if (clientConfig.testClientMatch.isDefined && servicesOnly.isDefined && !localConfigOnly) {
          log.error("You may use option servicesOnly only with localConfigOnly for client that requires preliminary testing")
          return InstallResult.Failure
        }
        log.info("Get client desired versions")
        val clientDesiredVersions = clientDistribution.downloadDesiredVersions().map(_.desiredVersions).getOrElse {
          log.warn(s"Can't get client desired versions")
          return InstallResult.Failure
        }
        var developerVersions = if (!localConfigOnly) {
          log.info("Get developer desired versions")
          val developerDesiredVersions = developerDistribution.downloadDesiredVersions().getOrElse {
            log.error(s"Can't get developer desired versions.")
            if (clientConfig.testClientMatch.isDefined) {
              log.error("May be developer desired versions are not tested")
            }
            return InstallResult.Failure
          }
          developerDesiredVersions.desiredVersions
        } else {
          clientDesiredVersions.mapValues(_.original())
        }
        log.info("Define versions to install")
        for (servicesOnly <- servicesOnly) {
          developerVersions = developerVersions.filterKeys(servicesOnly.contains(_))
        }
        developerVersions.foreach {
          case (serviceName, developerVersion) =>
            val existingVersions = clientDistribution.downloadVersionsInfo(serviceName).getOrElse {
              log.error(s"Error of getting service ${serviceName} versions list")
              return InstallResult.Failure
            }.versions
              .map(_.version)
              .filter(_.original() == developerVersion)
            val clientVersion =
              if (!localConfigOnly) {
                if (!existingVersions.isEmpty) {
                  developerVersions -= serviceName
                  clientDesiredVersions.get(serviceName) match {
                    case Some(clientVersion) if (developerVersion == clientVersion.original()) =>
                      clientVersion
                    case _ =>
                      existingVersions.sorted(BuildVersion.ordering.reverse).find(developerVersion == _.original()) match {
                        case Some(existingVersion) =>
                          existingVersion
                        case None =>
                          developerVersion
                      }
                  }
                } else {
                  developerVersion
                }
              } else {
                if (!existingVersions.isEmpty) {
                  existingVersions.sorted(BuildVersion.ordering).last.nextLocal()
                } else {
                  developerVersion
                }
              }
            clientVersions += (serviceName -> clientVersion)
        }
        log.info("Install updates")
        val result = installVersions(adminRepository, clientDistribution, developerDistribution,
          developerVersions, clientVersions, assignDesiredVersions)
        success = result != InstallResult.Failure
        result
      } catch {
        case ex: Exception =>
          log.error("Exception", ex)
          InstallResult.Failure
      } finally {
        adminRepository.processLogFile(success)
        if (!gitLock.unlock(ClientAdminRepository.makeEndOfUpdatesMessage(success, clientVersions))) {
          log.error("Can't unlock admin repository")
        }
        if (!clientVersions.isEmpty) {
          adminRepository.tagServices(clientVersions.map(_._1).toSeq)
        }
      }
    } else {
      log.error("Can't lock admin repository")
      InstallResult.Failure
    }
  }

  def getClientDesiredVersions(clientDistribution: ClientDistributionDirectoryClient): Option[Map[ServiceName, BuildVersion]] = {
    clientDistribution.downloadDesiredVersions().map(_.desiredVersions)
  }

  def setDesiredVersions(adminRepository: ClientAdminRepository,
                         clientDistribution: ClientDistributionDirectoryClient,
                         versions: Map[ServiceName, Option[BuildVersion]]): Boolean = {
    val gitLock = adminRepository.buildDesiredVersionsLock()
    if (gitLock.lock(AdminRepository.makeStartOfSettingDesiredVersionsMessage(versions),
        s"Continue of setting desired versions")) {
      var newDesiredVersions = Option.empty[DesiredVersions]
      try {
        val desiredVersionsMap = getClientDesiredVersions(clientDistribution).getOrElse {
          log.error("Error of getting desired versions")
          return false
        }
        val newVersions =
          versions.foldLeft(desiredVersionsMap) {
            (map, entry) => entry._2 match {
              case Some(version) =>
                map + (entry._1 -> version)
              case None =>
                map - entry._1
            }}
        val desiredVersions = new DesiredVersions(newVersions)
        if (!clientDistribution.uploadDesiredVersions(desiredVersions)) {
          log.error("Error of uploading desired versions")
          return false
        }
        newDesiredVersions = Some(desiredVersions)
        true
      } catch {
        case ex: Exception =>
          log.error("Exception", ex)
          false
      } finally {
        for (desiredVersions <- newDesiredVersions) {
          val desiredVersionsFile = adminRepository.getDesiredVersionsFile()
          if (!IoUtils.writeJsonToFile(desiredVersionsFile, desiredVersions.toJson)) {
            return false
          }
          if (!adminRepository.addFileToCommit(desiredVersionsFile)) {
            return false
          }
        }
        adminRepository.processLogFile(!newDesiredVersions.isEmpty)
        if (!gitLock.unlock(AdminRepository.makeEndOfSettingDesiredVersionsMessage(!newDesiredVersions.isEmpty))) {
          log.error("Can't unlock admin repository")
        }
        adminRepository.tagServices(versions.map(_._1).toSeq)
      }
    } else {
      log.error("Can't lock admin repository")
      false
    }
  }

  def signVersionsAsTested(adminRepository: ClientAdminRepository,
                           clientDistribution: ClientDistributionDirectoryClient,
                           developerDistribution: DeveloperDistributionDirectoryClient): Boolean = {
    val gitLock = adminRepository.buildDesiredVersionsLock()
    if (gitLock.lock(AdminRepository.makeStartOfSettingTestedFlagMessage(), AdminRepository.makeContinueOfSettingTestedFlagMessage())) {
      try {
        val clientDesiredVersionsMap = getClientDesiredVersions(clientDistribution).getOrElse {
          log.error("Error of getting client desired versions")
          return false
        }.mapValues(version => BuildVersion(version.client, version.build))
        val developerDesiredVersionsMap = developerDistribution.downloadDesiredVersions().getOrElse {
          log.error("Error of getting developer desired versions")
          return false
        }.desiredVersions
        if (developerDesiredVersionsMap.values.find(_.client.isDefined).isDefined) {
          log.error("Desired versions contain personal versions")
          return false
        }
        if (!clientDesiredVersionsMap.filter(!_._2.client.isDefined).equals(developerDesiredVersionsMap)) {
          log.error("Client versions are different from developer versions:")
          clientDesiredVersionsMap foreach {
            case (serviceName, version) =>
              developerDesiredVersionsMap.get(serviceName) match {
                case Some(commonVersion) if commonVersion != version =>
                  log.info(s"  service ${serviceName} version ${version} != ${commonVersion}")
                case _ =>
              }
          }
          developerDesiredVersionsMap foreach {
            case (serviceName, commonVersion) =>
              if (!clientDesiredVersionsMap.get(serviceName).isDefined) {
                log.info(s"  service ${serviceName} version ${commonVersion} is not installed")
              }
          }
          clientDesiredVersionsMap foreach {
            case (serviceName, version) =>
              if (!developerDesiredVersionsMap.get(serviceName).isDefined) {
                log.info(s"  service ${serviceName} is not the developer service")
              }
          }
          return false
        }
        if (!developerDistribution.uploadTestedVersions(ServicesVersions(clientDesiredVersionsMap))) {
          log.error("Error of uploading desired versions to developer")
          return false
        }
        true
      } catch {
        case ex: Exception =>
          log.error("Exception", ex)
          false
      } finally {
        if (!gitLock.unlock(AdminRepository.makeStopOfSettingTestedFlagMessage())) {
          log.error("Can't unlock admin repository")
        }
      }
    } else {
      log.error("Can't lock admin repository")
      false
    }
  }

  private def installVersions(adminRepository: ClientAdminRepository,
                              clientDistribution: ClientDistributionDirectoryClient,
                              developerDistribution: DeveloperDistributionDirectoryClient,
                              developerVersions: Map[ServiceName, BuildVersion],
                              clientVersions: Map[ServiceName, BuildVersion],
                              assignDesiredVersions: Boolean): InstallResult = {
    if (assignDesiredVersions && developerVersions.get(Common.InstallerServiceName).isDefined) {
      if (!installVersions(adminRepository, clientDistribution, developerDistribution,
        developerVersions.filterKeys(
          serviceName => {
            serviceName == Common.InstallerServiceName || serviceName == Common.DistributionServiceName
          }), clientVersions)) {
        return InstallResult.Failure
      }
      return InstallResult.NeedRestartToUpdate
    }
    if (!installVersions(adminRepository, clientDistribution, developerDistribution, developerVersions, clientVersions)) {
      return InstallResult.Failure
    }
    if (assignDesiredVersions) {
      log.info("Set desired versions")
      if (!setDesiredVersions(adminRepository, clientDistribution, clientVersions.map(entry => (entry._1, Some(entry._2))))) {
        log.error("Set desired versions error")
        return InstallResult.Failure
      }
      developerVersions.get(Common.DistributionServiceName) match {
        case Some(newDistributionVersion) =>
          if (!clientDistribution.waitForServerUpdated(clientDistribution.getDistributionVersionPath, newDistributionVersion)) {
            log.error("Update distribution server error")
            return InstallResult.Failure
          }
        case None =>
          for (newScriptsVersion <- developerVersions.get(Common.ScriptsServiceName)) {
            if (!clientDistribution.waitForServerUpdated(clientDistribution.getScriptsVersionPath, newScriptsVersion)) {
              log.error("Update scripts on distribution server error")
              return InstallResult.Failure
            }
          }
      }
    }
    InstallResult.Complete
  }

  private def installVersions(adminRepository: ClientAdminRepository,
                              clientDistribution: ClientDistributionDirectoryClient,
                              developerDistribution: DeveloperDistributionDirectoryClient,
                              developerVersions: Map[ServiceName, BuildVersion],
                              clientVersions: Map[ServiceName, BuildVersion]): Boolean = {
    developerVersions.foreach {
      case (serviceName, version) =>
        if (!installVersion(adminRepository, clientDistribution, developerDistribution, serviceName, version,
          clientVersions.get(serviceName).get)) {
          log.error(s"Can't install desired version ${version} of service ${serviceName}")
          return false
        }
    }
    true
  }

  private def installVersion(adminRepository: ClientAdminRepository,
                             clientDistribution: ClientDistributionDirectoryClient,
                             developerDirectory: DeveloperDistributionDirectoryClient,
                             serviceName: ServiceName, fromVersion: BuildVersion, toVersion: BuildVersion): Boolean = {
    try {
      log.info(s"Download version ${fromVersion} of service ${serviceName}")
      val versionInfo = developerDirectory.downloadVersionInfo(serviceName, fromVersion).getOrElse {
        log.error(s"Can't download version ${fromVersion} of service ${serviceName} info")
        return false
      }
      if (!IoUtils.deleteDirectoryContents(buildDir)) {
        log.error(s"Can't remove directory ${buildDir} contents")
        return false
      }
      if (!developerDirectory.downloadVersion(serviceName, fromVersion, buildDir)) {
        log.error(s"Can't download version ${fromVersion} of service ${serviceName}")
        return false
      }

      if (!Common.isUpdateService(serviceName) && !adminRepository.getServiceDir(serviceName).exists()) {
        log.error(s"Service ${serviceName} directory is not exist in the admin repository")
        return false
      }

      if (!mergeInstallConfigFile(adminRepository, serviceName)) {
        return false
      }

      log.info(s"Configure version ${toVersion} of service ${serviceName}")
      val configDir = adminRepository.getServiceSettingsDir(serviceName)
      if (configDir.exists()) {
        log.info(s"Merge private settings files")
        if (!mergeSettings(clientDistribution, serviceName, buildDir, configDir, toVersion)) {
          return false
        }
      }

      val privateDir = adminRepository.getServicePrivateDir(serviceName)
      if (privateDir.exists()) {
        log.info(s"Install private files")
        if (!IoUtils.copyFile(privateDir, buildDir)) {
          return false
        }
      }

      log.info(s"Upload version ${toVersion} of service ${serviceName}")
      val clientVersionInfo = BuildVersionInfo(versionInfo.author, versionInfo.branches, new Date(), versionInfo.comment)
      if (!clientDistribution.uploadVersion(serviceName, toVersion, clientVersionInfo, buildDir)) {
        return false
      }
      true
    } catch {
      case ex: Exception =>
        log.error("Install updates error", ex)
        false
    }
  }

  private def mergeInstallConfigFile(adminRepository: ClientAdminRepository, serviceName: ServiceName): Boolean = {
    val buildConfigFile = new File(buildDir, Common.InstallConfigFileName)
    val clientConfigFile = adminRepository.getServiceInstallConfigFile(serviceName)
    if (clientConfigFile.exists()) {
      log.info(s"Merge ${Common.InstallConfigFileName} with client version")
      val clientConfig = IoUtils.parseConfigFile(clientConfigFile).getOrElse(return false)
      if (buildConfigFile.exists()) {
        val buildConfig = IoUtils.parseConfigFile(buildConfigFile).getOrElse(return false)
        val newConfig = clientConfig.withFallback(buildConfig).resolve()
        IoUtils.writeConfigToFile(buildConfigFile, newConfig)
      } else {
        IoUtils.copyFile(buildConfigFile, clientConfigFile)
      }
    } else {
      true
    }
  }

  private def mergeSettings(clientDistribution: ClientDistributionDirectoryClient,
                            serviceName: ServiceName, buildDirectory: File, localDirectory: File,
                            version: BuildVersion, subPath: String = ""): Boolean = {
    for (localFile <- sortConfigFilesByIndex(new File(localDirectory, subPath).listFiles().toSeq)) {
      if (localFile.isDirectory) {
        val newSubPath = subPath + "/" + localFile.getName
        val buildSubDirectory = new File(buildDirectory, newSubPath)
        if (!buildSubDirectory.exists() && !buildSubDirectory.mkdir()) {
          log.error(s"Can't make ${buildSubDirectory}")
          return false
        }
        if (!mergeSettings(clientDistribution, serviceName, buildDirectory, localDirectory, version, newSubPath)) {
          return false
        }
      } else {
        val name = localFile.getName
        val originalName = getOriginalName(name)
        if (originalName.endsWith(".conf") || originalName.endsWith(".json") || originalName.endsWith(".properties")) {
          val filePath = if (subPath.isEmpty) originalName else subPath + "/" + originalName
          val buildConf = new File(buildDirectory, filePath)
          if (buildConf.exists()) {
            val configSettings = new ConfigSettings(IoUtils.parseConfigFile(localFile).getOrElse {
              return false
            })
            log.info(s"Merge configuration file ${filePath} with local configuration file ${localFile}")
            if (!configSettings.merge(buildConf)) {
              log.error("Merge configuration file error")
              return false
            }
          } else {
            log.info(s"Copy local configuration file ${localFile}")
            if (!IoUtils.copyFile(localFile, buildConf)) {
              return false
            }
          }
        } else if (originalName.endsWith(".defines")) {
          val sourceName = originalName.substring(0, originalName.length-8)
          val filePath = if (subPath.isEmpty) sourceName else subPath + "/" + sourceName
          val buildConf = new File(buildDirectory, filePath)
          var preSettings = Map.empty[String, String]
          preSettings += ("version" -> version.toString)
          preSettings += ("distribDirectoryUrl" -> clientDistribution.url.toString)
          val definesSettings = DefinesSettings(localFile, preSettings).getOrElse {
            return false
          }
          log.info(s"Extend configuration file ${filePath} with defines")
          if (!definesSettings.propertiesExpansion(buildConf)) {
            log.error("Extend configuration file with defines error")
            return false
          }
        } else {
          val filePath = if (subPath.isEmpty) name else subPath + "/" + name
          val buildConf = new File(buildDirectory, filePath)
          log.info(s"Copy local configuration file ${filePath}")
          if (!IoUtils.copyFile(localFile, buildConf)) {
            return false
          }
        }
      }
    }
    true
  }

  private def sortConfigFilesByIndex(files: Seq[File]): Seq[File] = {
    files.sortWith { (file1, file2) =>
      val (name1, index1) = file1.getName match {
        case indexPattern(name, index) => (name, index.toInt)
        case name => (name, 0)
      }
      val (name2, index2) = file2.getName match {
        case indexPattern(name, index) => (name, index.toInt)
        case name => (name, 0)
      }
      if (name1 != name2) {
        name1 < name2
      } else {
        index1 < index2
      }
    }
  }

  private def getOriginalName(name: String): String = {
    name match {
      case indexPattern(name, _) => name
      case name => name
    }
  }
}

object InstallResult extends Enumeration {
  type InstallResult = Value
  val Complete, Failure, NeedRestartToUpdate = Value
}
