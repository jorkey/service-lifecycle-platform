package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.common.common.Common.ServiceId
import com.vyulabs.update.common.config.{Repository, ServicePrivateFile}
import com.vyulabs.update.common.distribution.client.graphql.BuilderGraphqlCoder.{builderMutations, builderQueries}
import com.vyulabs.update.common.distribution.client.{SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.settings.{ConfigSettings, DefinedValues}
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
class ClientBuilder(builderDir: File) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  makeDir(builderDir)

  private val clientDir = makeDir(new File(builderDir, "client"))
  private val servicesDir = makeDir(new File(clientDir, "services"))

  def clientServiceDir(service: ServiceId) = makeDir(new File(servicesDir, service))
  def clientBuildDir(service: ServiceId) = makeDir(new File(clientServiceDir(service), "build"))
  def clientSettingsDir(service: ServiceId) = makeDir(new File(clientServiceDir(service), "settings"))

  private val indexPattern = "(.*)\\.([0-9]*)".r

  def buildClientVersion(distributionClient: SyncDistributionClient[SyncSource], service: ServiceId,
                         version: ClientDistributionVersion, author: String,
                         repositories: Seq[Repository], privateFiles: Seq[ServicePrivateFile],
                         values: Map[String, String])(implicit log: Logger): Boolean = {
    val developerVersion = DeveloperDistributionVersion.from(version)
    val versionInfo = downloadDeveloperVersion(distributionClient, service, developerVersion).getOrElse {
      log.error(s"Can't download developer version ${developerVersion} of service ${service}")
      return false
    }

    log.info(s"Prepare settings repositories of service ${service}")
    if (prepareSettingsRepositories(service, repositories).isEmpty) {
      log.error(s"Can't pull settings repositories")
      return false
    }

    log.info(s"Generate client version of service ${service}")
    if (!generateClientVersion(service, repositories.map(rep => rep.name +
        (rep.subDirectory match {
          case Some(dir) => "/" + dir
          case None => ""
        })), values)) {
      log.error(s"Can't generate client version ${version} of service ${service}")
      return false
    }

    log.info(s"Download private files")
    if (!downloadClientPrivateFiles(distributionClient, service, privateFiles)) {
      log.error(s"Can't download client private files")
      return false
    }

    log.info(s"Upload client version ${version} of service ${service}")
    uploadClientVersion(distributionClient, service, version, versionInfo.buildInfo, author)
  }

  private def prepareSettingsRepositories(service: ServiceId,
                                          repositories: Seq[Repository]): Option[Seq[GitRepository]] = {
    if (!IoUtils.deleteDirectoryContents(clientSettingsDir(service))) {
      return None
    }
    var gitRepositories = Seq.empty[GitRepository]
    for (sourceConfig <- repositories) {
      val branch = sourceConfig.git.branch
      val sourceRepository =
        GitRepository.getGitRepository(sourceConfig.git.url, branch, sourceConfig.git.cloneSubmodules.getOrElse(true),
            new File(clientSettingsDir(service), sourceConfig.name)).getOrElse {
          return None
        }
      gitRepositories :+= sourceRepository
    }
    Some(gitRepositories)
  }

  def uploadClientVersion(distributionClient: SyncDistributionClient[SyncSource], service: ServiceId,
                          version: ClientDistributionVersion, buildInfo: BuildInfo, author: String)(implicit log: Logger): Boolean = {
    if (!ZipUtils.zipAndSend(clientBuildDir(service), file => distributionClient.uploadClientVersionImage(service, version, file))) {
      return false
    }
    val clientVersionInfo = ClientVersionInfo.from(service, version, buildInfo, InstallInfo(author, new Date()))
    if (!distributionClient.graphqlRequest(builderMutations.addClientVersionInfo(clientVersionInfo)).getOrElse(false)) {
      return false
    }
    true
  }

  def downloadDeveloperVersion(distributionClient: SyncDistributionClient[SyncSource], service: ServiceId,
                               version: DeveloperDistributionVersion)(implicit log: Logger): Option[DeveloperVersionInfo] = {
    log.info(s"Get developer version ${version} of service ${service} info")
    val versionInfo = distributionClient.graphqlRequest(
        builderQueries.getDeveloperVersionsInfo(service,
          distribution = Some(version.distribution), version = Some(version.developerVersion))).getOrElse(Seq.empty).headOption.getOrElse {
      log.error(s"Can't get developer version ${version} of service ${service} info")
      return None
    }
    if (!IoUtils.deleteDirectoryContents(clientBuildDir(service))) {
      log.error(s"Can't remove directory ${clientBuildDir(service)} contents")
      return None
    }
    log.info(s"Download developer version ${version} of service ${service}")
    if (!ZipUtils.receiveAndUnzip(file => distributionClient.downloadDeveloperVersionImage(service, version, file), clientBuildDir(service))) {
      log.error(s"Can't download developer version ${version} of service ${service}")
      return None
    }
    Some(versionInfo)
  }

  def generateClientVersion(service: ServiceId, settingsDirs: Seq[String], values: Map[String, String])
                           (implicit log: Logger): Boolean = {
    if (!settingsDirs.isEmpty) {
      log.info(s"Configure client version of service ${service}")
      settingsDirs.foreach { dir =>
        val settingDir = new File(clientSettingsDir(service), dir)
        if (settingDir.exists()) {
          log.info(s"Merge private settings files")
          if (!mergeSettings(service, clientBuildDir(service), settingDir, values)) {
            return false
          }
        }
      }
    }
    true
  }

  def downloadClientPrivateFiles(distributionClient: SyncDistributionClient[SyncSource], service: ServiceId,
                                 serviceFiles: Seq[ServicePrivateFile])
                                (implicit log: Logger): Boolean = {
    if (!serviceFiles.isEmpty) {
      serviceFiles.foreach { serviceFile =>
        log.info(s"Download client private file ${serviceFile.loadPath}")
        val file = new File(clientBuildDir(service), serviceFile.path)
        file.getParentFile.mkdirs()
        if (!distributionClient.downloadClientPrivateFile(serviceFile.loadPath, file)) {
          return false
        }
      }
    }
    true
  }

  def makeClientVersionImage(service: ServiceId): Option[File] = {
    val directory = clientBuildDir(service)
    val file = Files.createTempFile(s"${service}-version", "zip").toFile
    file.deleteOnExit()
    if (ZipUtils.zip(file, directory)) {
      Some(file)
    } else {
      None
    }
  }

  private def mergeSettings(service: ServiceId, buildDirectory: File, localDirectory: File,
                            values: Map[String, String], subPath: String = "")(implicit log: Logger): Boolean = {
    for (localFile <- sortConfigFilesByIndex(new File(localDirectory, subPath).listFiles().toSeq)) {
      if (localFile.isDirectory) {
        val newSubPath = subPath + "/" + localFile.getName
        val buildSubDirectory = new File(buildDirectory, newSubPath)
        if (!buildSubDirectory.exists() && !buildSubDirectory.mkdir()) {
          log.error(s"Can't make ${buildSubDirectory}")
          return false
        }
        if (!mergeSettings(service, buildDirectory, localDirectory, values, newSubPath)) {
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
          val definedValues = DefinedValues(localFile, values).getOrElse {
            return false
          }
          log.info(s"Extend configuration file ${filePath} with defines")
          if (!definedValues.propertiesExpansion(buildConf)) {
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