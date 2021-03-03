package com.vyulabs.update.builder

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries}
import com.vyulabs.update.common.distribution.client.{SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.settings.{ConfigSettings, DefinesSettings}
import com.vyulabs.update.common.utils.Utils.makeDir
import com.vyulabs.update.common.utils.{IoUtils, ZipUtils}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.nio.file.Files
import java.util.Date

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class ClientBuilder(builderDir: File, val distributionName: DistributionName) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val clientDir = makeDir(new File(builderDir, "client"))
  private val servicesDir = makeDir(new File(clientDir, "services"))

  private val settingsDirectory = new SettingsDirectory(builderDir, distributionName)

  def clientServiceDir(serviceName: ServiceName) = makeDir(new File(servicesDir, serviceName))
  def clientBuildDir(serviceName: ServiceName) = makeDir(new File(clientServiceDir(serviceName), "build"))

  private val indexPattern = "(.*)\\.([0-9]*)".r

  def buildClientVersion(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName,
                         developerVersion: DeveloperDistributionVersion, clientVersion: ClientDistributionVersion,
                         author: String, arguments: Map[String, String])(implicit log: Logger): Boolean = {
    val versionInfo = downloadDeveloperVersion(distributionClient, serviceName, developerVersion).getOrElse {
      log.error(s"Can't download developer version ${developerVersion} of service ${serviceName}")
      return false
    }

    if (!generateClientVersion(serviceName, arguments)) {
      log.error(s"Can't generate client version ${clientVersion} of service ${serviceName}")
      return false
    }

    log.info(s"Upload client version ${clientVersion} of service ${serviceName}")
    uploadClientVersion(distributionClient, serviceName, clientVersion,
      author, versionInfo.buildInfo)
  }

  def uploadClientVersion(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName,
                          version: ClientDistributionVersion, author: String): Boolean = {
    val buildInfo = BuildInfo(author, Seq.empty, new Date(), Some("Initial version"))
    uploadClientVersion(distributionClient, serviceName, version, author, buildInfo)
  }

  def uploadClientVersion(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName,
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

  def setDesiredVersions(distributionClient: SyncDistributionClient[SyncSource], versions: Seq[ClientDesiredVersionDelta]): Boolean = {
    distributionClient.graphqlRequest(administratorMutations.setClientDesiredVersions(versions)).getOrElse(false)
  }

  def downloadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource], serviceName: ServiceName,
                               version: DeveloperDistributionVersion)(implicit log: Logger): Option[DeveloperVersionInfo] = {
    log.info(s"Get developer version ${version} of service ${serviceName} info")
    val versionInfo = distributionClient.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(serviceName)).getOrElse(Seq.empty).headOption.getOrElse {
      log.error(s"Can't get developer version ${version} of service ${serviceName} info")
      return None
    }
    if (!IoUtils.deleteDirectoryContents(clientBuildDir(serviceName))) {
      log.error(s"Can't remove directory ${clientBuildDir(serviceName)} contents")
      return None
    }
    log.info(s"Download developer version ${version} of service ${serviceName}")
    if (!ZipUtils.receiveAndUnzip(file => distributionClient.downloadDeveloperVersionImage(serviceName, version, file), clientBuildDir(serviceName))) {
      log.error(s"Can't download developer version ${version} of service ${serviceName}")
      return None
    }
    Some(versionInfo)
  }

  def generateClientVersion(serviceName: ServiceName, arguments: Map[String, String])(implicit log: Logger): Boolean = {
    if (!mergeInstallConfigFile(serviceName)) {
      return false
    }

    log.info(s"Configure client version of service ${serviceName}")
    val configDir = settingsDirectory.getServiceSettingsDir(serviceName)
    if (configDir.exists()) {
      log.info(s"Merge private settings files")
      if (!mergeSettings(serviceName, clientBuildDir(serviceName), configDir, arguments)) {
        return false
      }
    }

    val privateDir = settingsDirectory.getServicePrivateDir(serviceName)
    if (privateDir.exists()) {
      log.info(s"Copy private files")
      if (!IoUtils.copyFile(privateDir, clientBuildDir(serviceName))) {
        return false
      }
    }
    true
  }

  def makeClientVersionImage(serviceName: ServiceName): Option[File] = {
    val directory = clientBuildDir(serviceName)
    val file = Files.createTempFile(s"${serviceName}-version", "zip").toFile
    file.deleteOnExit()
    if (ZipUtils.zip(file, directory)) {
      Some(file)
    } else {
      None
    }
  }

  private def mergeInstallConfigFile(serviceName: ServiceName)(implicit log: Logger): Boolean = {
    val buildConfigFile = new File(clientBuildDir(serviceName), Common.InstallConfigFileName)
    val clientConfigFile = settingsDirectory.getServiceInstallConfigFile(serviceName)
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