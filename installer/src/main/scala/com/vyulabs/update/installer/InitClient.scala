package com.vyulabs.update.installer

import java.io.File
import java.net.{URI, URL}

import com.vyulabs.update.distribution.distribution.ClientAdminRepository
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ServiceName}
import com.vyulabs.update.distribution.{DistributionDirectory, DistributionDirectoryClient}
import com.vyulabs.update.installer.config.InstallerConfig
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, ProcessUtils, ZipUtils}
import com.vyulabs.update.version.DeveloperDistributionVersion
import org.slf4j.Logger
import com.vyulabs.update.installer.config.InstallerConfig._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class InitClient()(implicit filesLocker: SmartFilesLocker, log: Logger) {
  private val adminRepositoryDir = new File("..", "admin")
  private val distributionDir = new File("..", "distrib")

  def initClient(cloudProvider: String, distributionName: String,
                 adminRepositoryUrl: URI, developerDistributionUrl: URL, clientDistributionUrl: URL,
                 distributionServicePort: Int): Boolean = {
    val developerDistribution = new DistributionDirectoryClient(developerDistributionUrl)
    val clientDistribution = new DistributionDirectory(new File(distributionDir, "directory"))
    log.info("Init admin repository")
    if (!initAdminRepository()) {
      log.error("Can't init admin repository")
      return false
    }
    log.info("Init distribution directory")
    if (!initDistribDirectory(cloudProvider, distributionName, clientDistribution, developerDistribution, distributionServicePort)) {
      log.error("Can't init distribution directory")
      return false
    }
    log.info("Init install directory")
    /* TODO graphql
    if (!initInstallDirectory(
        clientDistribution.getVersionImageFile(Common.ScriptsServiceName,
        clientDistribution.getDesiredVersion(Common.ScriptsServiceName).get),
        adminRepositoryUrl, developerDistributionUrl, clientDistributionUrl)) {
      log.error("Can't init install repository")
      return false
    }*/
    log.info("Client is initialized successfully.")
    true
  }

  private def initAdminRepository(): Boolean = {
    if (!adminRepositoryDir.exists()) {
      log.info(s"Create admin repository in directory ${adminRepositoryDir}")
      if (!adminRepositoryDir.mkdir()) {
        log.error(s"Can't make directory ${adminRepositoryDir}")
        return false
      }
      if (!ClientAdminRepository.create(adminRepositoryDir)) {
        log.error("Can't create admin repository")
        return false
      }
    } else {
      log.info(s"Directory ${adminRepositoryDir} exists")
    }
    true
  }

  private def initInstallDirectory(scriptsZip: File,
                                   adminRepositoryUrl: URI, developerDistributionUrl: URL, clientDistributionUrl: URL): Boolean = {
    log.info(s"Create ${InstallerConfig.configFile}")
    val config = InstallerConfig(adminRepositoryUrl, developerDistributionUrl, clientDistributionUrl)
    if (!IoUtils.writeJsonToFile(InstallerConfig.configFile, config)) {
      return false
    }
    log.info("Update installer.sh")
    if (!ZipUtils.unzip(scriptsZip, new File("."), (name: String) => {
      if (name == "installer/installer.sh") {
        Some("installer.sh")
      } else {
        None
      }
    })) {
      return false
    }
    val installerFile = new File("installer.sh")
    val content = new String(IoUtils.readFileToBytes(installerFile).getOrElse {
      log.error(s"Read file ${installerFile} error")
      return false
    }, "utf8")
    installerFile.renameTo(File.createTempFile("installer", "sh"))
    if (!IoUtils.writeBytesToFile(installerFile, content.getBytes("utf8"))) {
      log.error(s"Write file ${installerFile} error")
      return false
    }
    if (!ProcessUtils.runProcess("chmod", Seq("+x", "installer.sh"), Map.empty, new File("."),
          Some(0), None, ProcessUtils.Logging.None)) {
      log.warn("Can't set execution attribute to installer.sh")
    }
    true
  }

  private def initDistribDirectory(cloudProvider: String, name: String,
                                   clientDistribution: DistributionDirectory,
                                   developerDistribution: DistributionDirectoryClient,
                                   distributionServicePort: Int): Boolean = {
    if (!distributionDir.exists()) {
      log.info(s"Create directory ${distributionDir}")
      if (!distributionDir.mkdir()) {
        log.error(s"Can't make directory ${distributionDir}")
        return false
      }
    } else {
      log.info(s"Directory ${distributionDir} exists")
    }
    log.info("Download desired versions")
    val desiredVersions = developerDistribution.downloadDeveloperDesiredVersionsForMe().getOrElse {
      log.error("Can't download desired versions")
      return false
    }
    val desiredVersionsMap = desiredVersions
    if (!downloadUpdateServices(clientDistribution, developerDistribution, desiredVersionsMap)) {
      log.error("Can't download update services")
      return false
    }
    log.info("Write desired versions")
    /* TODO graphql
    if (!IoUtils.writeJsonToFile(clientDistribution.getDesiredVersionsFile(), desiredVersions)) {
      log.error("Can't write desired versions")
      return false
    }*/
    log.info("Setup distribution server")
    if (!setupDistributionServer(cloudProvider, name, clientDistribution, developerDistribution,
        desiredVersionsMap, distributionServicePort)) {
      log.error("Can't setup distribution server")
      return false
    }
    true
  }

  private def downloadUpdateServices(clientDistribution: DistributionDirectory,
                                     developerDistribution: DistributionDirectoryClient,
                                     desiredVersions: Map[ServiceName, DeveloperDistributionVersion]): Boolean = {
    Seq(Common.ScriptsServiceName, Common.DistributionServiceName, Common.InstallerServiceName, Common.UpdaterServiceName).foreach {
      serviceName =>
        /* TODO graphql
        if (!downloadVersion(clientDistribution, developerDistribution, serviceName, desiredVersions.get(serviceName).getOrElse {
          log.error(s"Desired version of service ${serviceName} is not defined")
          return false
        })) {
          log.error(s"Can't copy version image of service ${serviceName}")
          return false
        }*/
        null
    }
    true
  }

  private def setupDistributionServer(cloudProvider: String, name: String,
                                      clientDistribution: DistributionDirectory,
                                      developerDistribution: DistributionDirectoryClient,
                                      desiredVersions: Map[ServiceName, DeveloperDistributionVersion],
                                      distributionServicePort: Int): Boolean = {
    ZipUtils.unzip(clientDistribution.getDeveloperVersionImageFile(Common.ScriptsServiceName, desiredVersions.get(Common.ScriptsServiceName).get),
      distributionDir, (name: String) => {
        if (name == "distribution/distribution_setup.sh") {
          Some("distribution_setup.sh")
        } else {
          None
        }})
    if (!ProcessUtils.runProcess("bash",
        Seq( "./distribution_setup.sh", "client", cloudProvider, name, distributionServicePort.toString, developerDistribution.url.toString),
        Map.empty, distributionDir, Some(0), None, ProcessUtils.Logging.Realtime)) {
      log.error("Can't setup distribution server")
      return false
    }
    new File("distribution_setup.sh").delete()
    true
  }
}
