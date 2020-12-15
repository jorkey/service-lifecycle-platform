package com.vyulabs.update.builder

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.distribution.client.SyncDistributionClient
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries}
import com.vyulabs.update.common.distribution.client.graphql.DistributionGraphqlCoder.{distributionMutations, distributionQueries}
import com.vyulabs.update.common.info.{BuildInfo, ClientDesiredVersions, ClientVersionInfo, DeveloperDesiredVersions, DeveloperVersionInfo, InstallInfo}
import com.vyulabs.update.common.settings.{ConfigSettings, DefinesSettings}
import com.vyulabs.update.common.utils.{IoUtils, ZipUtils}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import com.vyulabs.update.common.utils.Utils.makeDir
import com.vyulabs.update.distribution.SettingsDirectory
import org.slf4j.Logger

import java.io.File
import spray.json.DefaultJsonProtocol._

import java.util.Date

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
object ClientBuilder {
  private val clientDir = makeDir(new File("client"))
  private val servicesDir = makeDir(new File(clientDir, "services"))

  def clientServiceDir(serviceName: ServiceName) = makeDir(new File(servicesDir, serviceName))
  def clientBuildDir(serviceName: ServiceName) = makeDir(new File(clientServiceDir(serviceName), "build"))

  private val indexPattern = "(.*)\\.([0-9]*)".r

  def downloadUpdates(distributionClient: SyncDistributionClient, developerDistributionClient: SyncDistributionClient, serviceNames: Seq[ServiceName])
                     (implicit log: Logger): Boolean = {
    val developerDesiredVersions = developerDistributionClient.graphqlRequest(distributionQueries.getDesiredVersions(serviceNames))
        .map(DeveloperDesiredVersions.toMap(_)).getOrElse {
      log.error(s"Can't get developer desired versions.")
      return false
    }
    developerDesiredVersions.foreach {
      case (serviceName, version) if version.distributionName == developerDistributionClient.distributionName =>
        log.info(s"Download version ${version}")
        val imageFile = File.createTempFile("version", "image")
        try {
          if (!developerDistributionClient.downloadDeveloperVersionImage(serviceName, version, imageFile)) {
            log.error(s"Can't download developer version ${version} image file ${imageFile}")
            return false
          }
          val versionInfo = developerDistributionClient.graphqlRequest(distributionQueries.getVersionsInfo(serviceName, None, Some(version)))
            .map(_.headOption).flatten.getOrElse {
            log.error(s"Can't get developer version ${version} info}")
            return false
          }
          if (!distributionClient.uploadDeveloperVersionImage(serviceName, version, imageFile)) {
            log.error(s"Can't upload developer version ${version} image file ${imageFile}")
            return false
          }
          if (!distributionClient.graphqlRequest(administratorMutations.addDeveloperVersionInfo(versionInfo)).getOrElse(false)) {
            log.error(s"Can't add developer version ${version} info")
            return false
          }
        } finally {
          imageFile.delete()
        }
      case _ =>
    }
    true
  }

  def buildClientVersions(distributionClient: SyncDistributionClient, settingsRepository: SettingsDirectory,
                          serviceNames: Seq[ServiceName], author: String, arguments: Map[String, String])
                         (implicit log: Logger): Map[ServiceName, ClientDistributionVersion] = {
    var clientVersions = Map.empty[ServiceName, ClientDistributionVersion]
    log.info("Get developer desired versions")
    val developerDesiredVersions = distributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions(serviceNames))
        .map(DeveloperDesiredVersions.toMap(_)).getOrElse {
      log.error(s"Can't get developer desired versions.")
      return Map.empty
    }
    developerDesiredVersions.foreach {
      case (serviceName, developerVersion) =>
        val existingVersions = distributionClient.graphqlRequest(administratorQueries.getClientVersionsInfo(serviceName)).getOrElse {
          log.error(s"Error of getting service ${serviceName} client versions list")
          return Map.empty
        }.map(_.version).filter(_.original() == developerVersion)
        val clientVersion =
          if (!existingVersions.isEmpty) {
            existingVersions.sorted(ClientDistributionVersion.ordering).last.next()
          } else {
            ClientDistributionVersion(developerVersion)
          }
        clientVersions += (serviceName -> clientVersion)
    }
    log.info("Build client versions")
    developerDesiredVersions.foreach {
      case (serviceName, version) =>
        log.info(s"Build client version ${version} of service ${serviceName}")
        if (!buildClientVersion(distributionClient, settingsRepository, serviceName, version, clientVersions.get(serviceName).get, author,
              arguments + ("version" -> version.toString))) {
          log.error(s"Can't build client versions")
          return Map.empty
        }
    }
    clientVersions
  }

  def downloadDeveloperVersion(distributionClient: SyncDistributionClient, serviceName: ServiceName,
                               version: DeveloperDistributionVersion)(implicit log: Logger): Option[DeveloperVersionInfo] = {
    log.info(s"Get developer version ${version} of service ${serviceName} info")
    val versionInfo = distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName)).getOrElse(Seq.empty).headOption.getOrElse {
      log.error(s"Can't get developer version ${version} of service ${serviceName} info")
      return None
    }
    if (!IoUtils.deleteDirectoryContents(clientServiceDir(serviceName))) {
      log.error(s"Can't remove directory ${clientServiceDir(serviceName)} contents")
      return None
    }
    log.info(s"Download developer version ${version} of service ${serviceName}")
    if (!ZipUtils.receiveAndUnzip(file => distributionClient.downloadDeveloperVersionImage(serviceName, version, file), clientBuildDir(serviceName))) {
      log.error(s"Can't download developer version ${version} of service ${serviceName}")
      return None
    }
    Some(versionInfo)
  }

  def buildClientVersion(distributionClient: SyncDistributionClient, settingsRepository: SettingsDirectory, serviceName: ServiceName,
                         fromVersion: DeveloperDistributionVersion, toVersion: ClientDistributionVersion, author: String,
                         arguments: Map[String, String])(implicit log: Logger): Boolean = {
    val versionInfo = downloadDeveloperVersion(distributionClient, serviceName, fromVersion).getOrElse {
      log.error(s"Can't download developer version ${fromVersion} of service ${serviceName}")
      return false
    }

    if (!generateClientVersion(settingsRepository, serviceName, arguments)) {
      log.error(s"Can't generate client version ${toVersion} of service ${serviceName}")
      return false
    }

    log.info(s"Upload client version ${toVersion} of service ${serviceName}")
    uploadClientVersion(distributionClient, serviceName, toVersion, author, versionInfo.buildInfo)
  }

  def uploadClientVersion(distributionClient: SyncDistributionClient, serviceName: ServiceName,
                          version: ClientDistributionVersion, author: String, buildInfo: BuildInfo)(implicit log: Logger): Boolean = {
    if (!ZipUtils.zipAndSend(clientBuildDir(serviceName), file => distributionClient.uploadClientVersionImage(serviceName, version, file))) {
      return false
    }
    val clientVersionInfo = ClientVersionInfo(serviceName, version, buildInfo, InstallInfo(author, new Date()))
    if (!distributionClient.graphqlRequest(administratorMutations.addClientVersionInfo(clientVersionInfo)).getOrElse(false)) {
      return false
    }
    true
  }

  def generateClientVersion(settingsRepository: SettingsDirectory, serviceName: ServiceName,
                            arguments: Map[String, String])(implicit log: Logger): Boolean = {
    if (!settingsRepository.getServiceDir(serviceName).exists()) {
      log.error(s"Service ${serviceName} directory is not exist in the admin repository")
      return false
    }

    if (!mergeInstallConfigFile(settingsRepository, serviceName)) {
      return false
    }

    log.info(s"Configure client version of service ${serviceName}")
    val configDir = settingsRepository.getServiceSettingsDir(serviceName)
    if (configDir.exists()) {
      log.info(s"Merge private settings files")
      if (!mergeSettings(serviceName, clientBuildDir(serviceName), configDir, arguments)) {
        return false
      }
    }

    val privateDir = settingsRepository.getServicePrivateDir(serviceName)
    if (privateDir.exists()) {
      log.info(s"Copy private files")
      if (!IoUtils.copyFile(privateDir, clientBuildDir(serviceName))) {
        return false
      }
    }
    true
  }

  def setClientDesiredVersions(distributionClient: SyncDistributionClient, versions: Map[ServiceName, Option[ClientDistributionVersion]])
                              (implicit log: Logger): Boolean = {
    val desiredVersionsMap = ClientDesiredVersions.toMap(distributionClient.graphqlRequest(administratorQueries.getClientDesiredVersions()).getOrElse {
      log.error("Error of getting desired versions")
      return false
    })
    val newVersions =
      versions.foldLeft(desiredVersionsMap) {
        (map, entry) => entry._2 match {
          case Some(version) =>
            map + (entry._1 -> version)
          case None =>
            map - entry._1
        }}
    val desiredVersions = ClientDesiredVersions.fromMap(newVersions)
    if (!distributionClient.graphqlRequest(administratorMutations.setClientDesiredVersions(desiredVersions)).getOrElse(false)) {
      log.error("Error of uploading desired versions")
      return false
    }
    true
  }

  def signVersionsAsTested(distributionClient: SyncDistributionClient, developerDistribution: SyncDistributionClient)
                          (implicit log: Logger): Boolean = {
    val clientDesiredVersionsMap = distributionClient.graphqlRequest(administratorQueries.getClientDesiredVersions())
        .map(ClientDesiredVersions.toMap(_)).getOrElse {
      log.error("Error of getting client desired versions")
      return false
    }
    val developerDesiredVersionsMap = developerDistribution.graphqlRequest(distributionQueries.getDesiredVersions())
        .map(DeveloperDesiredVersions.toMap(_)).getOrElse {
      log.error("Error of getting developer desired versions")
      return false
    }
    if (!clientDesiredVersionsMap.filter(_._2.distributionName == developerDistribution.distributionName)
        .mapValues(_.original()).equals(developerDesiredVersionsMap)) {
      log.error("Client versions are different from developer versions:")
      clientDesiredVersionsMap foreach {
        case (serviceName, clientVersion) =>
          developerDesiredVersionsMap.get(serviceName) match {
            case Some(developerVersion) if developerVersion != clientVersion.original() =>
              log.info(s"  service ${serviceName} version ${clientVersion} != ${developerVersion}")
            case _ =>
          }
      }
      developerDesiredVersionsMap foreach {
        case (serviceName, developerVersion) =>
          if (!clientDesiredVersionsMap.get(serviceName).isDefined) {
            log.info(s"  service ${serviceName} version ${developerVersion} is not installed")
          }
      }
      clientDesiredVersionsMap foreach {
        case (serviceName, _) =>
          if (!developerDesiredVersionsMap.get(serviceName).isDefined) {
            log.info(s"  service ${serviceName} is not the developer service")
          }
      }
      return false
    }
    if (!developerDistribution.graphqlRequest(distributionMutations.setTestedVersions(
        DeveloperDesiredVersions.fromMap(clientDesiredVersionsMap.mapValues(_.original())))).getOrElse(false)) {
      log.error("Error of uploading desired versions to developer")
      return false
    }
    true
  }

  def waitForServerUpdated(distributionClient: SyncDistributionClient,
                           serviceName: ServiceName, desiredVersion: ClientDistributionVersion, waitingTimeoutSec: Int = 10000)(implicit log: Logger): Boolean = {
    log.info(s"Wait for distribution server updated")
    for (_ <- 0 until waitingTimeoutSec) {
      if (distributionClient.available()) {
        distributionClient.getServiceVersion(serviceName) match {
          case Some(version) =>
            if (version == desiredVersion) {
              log.info(s"Distribution server is updated")
              return true
            }
          case None =>
            return false
        }
      }
      Thread.sleep(1000)
    }
    log.error(s"Timeout of waiting for distribution server become available")
    false
  }

  private def mergeInstallConfigFile(adminRepository: SettingsDirectory, serviceName: ServiceName)
                                    (implicit log: Logger): Boolean = {
    val buildConfigFile = new File(clientBuildDir(serviceName), Common.InstallConfigFileName)
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

  private def mergeSettings(serviceName: ServiceName, buildDirectory: File, localDirectory: File,
                            arguments: Map[String, String], subPath: String = "")(implicit log: Logger): Boolean = {
    for (localFile <- sortConfigFilesByIndex(new File(localDirectory, subPath).listFiles().toSeq)) {
      if (localFile.isDirectory) {
        val newSubPath = subPath + "/" + localFile.getName
        val buildSubDirectory = new File(buildDirectory, newSubPath)
        if (!buildSubDirectory.exists() && !buildSubDirectory.mkdir()) {
          log.error(s"Can't make ${buildSubDirectory}")
          return false
        }
        if (!mergeSettings(serviceName, buildDirectory, localDirectory, arguments, newSubPath)) {
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
          val definesSettings = DefinesSettings(localFile, arguments).getOrElse {
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