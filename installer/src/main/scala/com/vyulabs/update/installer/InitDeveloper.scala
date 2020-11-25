package com.vyulabs.update.installer

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.distribution.server.DistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, ProcessUtils, ZipUtils}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.05.19.
  * Copyright FanDate, Inc.
  */
class InitDeveloper()(implicit filesLocker: SmartFilesLocker, log: Logger) {
  private val buildDir = new File("..", "build")
  private val distributionDir = new File("..", "distrib")

  def initDeveloper(distributionServicePort: Int): Boolean = {
    log.info("Init admin repository")
    log.info("Init build directory")
    if (!initBuildDirectory()) {
      log.error("Can't init build repository")
      return false
    }
    log.info("Init distribution directory")
    if (!initDistribDirectory(distributionServicePort)) {
      log.error("Can't init distribution directory")
      return false
    }
    log.info("Developer is initialized successfully.")
    true
  }

  private def initBuildDirectory(): Boolean = {
    if (!buildDir.exists()) {
      log.info(s"Create directory ${buildDir}")
      if (!buildDir.mkdir()) {
        log.error(s"Can't make directory ${buildDir}")
        return false
      }
    } else {
      log.info(s"Directory ${buildDir} exists")
    }
    log.info("Update builder.sh")
    val scriptsZip = new File("scripts.zip")
    if (!ZipUtils.unzip(scriptsZip, buildDir, (name: String) => {
      if (name == "builder/builder.sh") {
        Some("builder.sh")
      } else {
        None
      }
    })) {
      return false
    }
    true
  }

  private def initDistribDirectory(distributionServicePort: Int): Boolean = {
    if (!distributionDir.exists()) {
      log.info(s"Create directory ${distributionDir}")
      if (!distributionDir.mkdir()) {
        log.error(s"Can't make directory ${distributionDir}")
        return false
      }
    } else {
      log.info(s"Directory ${distributionDir} exists")
    }
    log.info(s"Init directory ${distributionDir}")
    val developerDistribution = new DistributionDirectory(new File(distributionDir, "directory"))
    log.info("Copy scripts")
    if (!copyScripts(developerDistribution)) {
      log.error("Can't copy scripts")
      return false
    }
    log.info("Copy update services")
    if (!copyUpdateServices(developerDistribution)) {
      log.error("Can't copy update services")
      return false
    }
    log.info("Setup distribution server")
    if (!setupDistributionServer(distributionServicePort)) {
      log.info("Can't setup distribution server")
    }
    true
  }

  private def copyUpdateServices(developerDistribution: DistributionDirectory): Boolean = {
    Seq(Common.DistributionServiceName, Common.BuilderServiceName, Common.InstallerServiceName, Common.UpdaterServiceName).foreach {
      serviceName =>
        if (!copyUpdateService(serviceName, developerDistribution)) {
          log.error(s"Can't copy version image of service ${serviceName}")
          return false
        }
    }
    true
  }

  private def copyScripts(developerDistribution: DistributionDirectory): Boolean = {
    /* TODO graphql
    val nextVersion = developerDistribution.getDesiredVersion(Common.ScriptsServiceName) match {
      case Some(version) => version.next()
      case None => BuildVersion(1, 0, 0)
    }
    log.info(s"Create version ${nextVersion} of service ${Common.ScriptsServiceName}")
    val scriptsZip = new File("scripts.zip")
    if (!IoUtils.copyFile(scriptsZip, developerDistribution.getVersionImageFile(Common.ScriptsServiceName, nextVersion))) {
      log.error("Can't copy scripts jar file")
      return false
    }
    val scriptsVersionInfo = BuildVersionInfo("administrator", Seq.empty, new Date(), Some("Initial version"))
    if (!IoUtils.writeJsonToFile(developerDistribution.getVersionInfoFile(Common.ScriptsServiceName, nextVersion), scriptsVersionInfo)) {
      log.error(s"Can't write scripts version info")
      return false
    }
    var desiredVersions = developerDistribution.getDesiredVersions(None).map(_.toMap).getOrElse(Map.empty)
    desiredVersions += Common.ScriptsServiceName -> nextVersion
    if (!IoUtils.writeJsonToFile(developerDistribution.getDesiredVersionsFile(None), DesiredVersions.fromMap(desiredVersions))) {
      log.error("Can't write desired versions")
      return false
    }*/
    true
  }

  private def copyUpdateService(serviceName: ServiceName, developerDistribution: DistributionDirectory): Boolean = {
    /* TODO graphql
    val sourceJar = new File(s"${serviceName.toString}.jar")
    if (sourceJar.exists()) {
      val nextVersion = developerDistribution.getDesiredVersion(serviceName) match {
        case Some(version) => version.next()
        case None => BuildVersion(1, 0, 0)
      }
      log.info(s"Create version ${nextVersion} of service ${serviceName}")
      val imageJar = new File(s"${serviceName}-${nextVersion.toString}.jar")
      if (!IoUtils.copyFile(sourceJar, imageJar)) {
        log.error(s"Can't copy jar of service ${serviceName}")
        return false
      }
      val distributionZip = developerDistribution.getVersionImageFile(serviceName, nextVersion)
      if (!ZipUtils.zip(distributionZip, imageJar)) {
        log.error(s"Can't zip image of service ${serviceName}")
        return false
      }
      imageJar.delete()
      val versionInfo = BuildVersionInfo("administrator", Seq.empty, new Date(), Some("Initial version"))
      if (!IoUtils.writeJsonToFile(developerDistribution.getVersionInfoFile(serviceName, nextVersion), versionInfo)) {
        log.error(s"Can't write version info of service ${serviceName}")
        return false
      }
      var desiredVersions = developerDistribution.getDesiredVersions(None).map(_.toMap).getOrElse(Map.empty)
      desiredVersions += serviceName -> nextVersion
      if (!IoUtils.writeJsonToFile(developerDistribution.getDesiredVersionsFile(None), DesiredVersions.fromMap(desiredVersions))) {
        log.error("Can't write desired versions")
        return false
      }
      true
    } else {
      log.error(s"File ${sourceJar} is not exist")
      false
    }
    */
    false
  }

  private def setupDistributionServer(distributionServicePort: Int): Boolean = {
    val scriptsZip = new File("scripts.zip")
    if (!ZipUtils.unzip(scriptsZip, distributionDir, (name: String) => {
        if (name == "distribution/distribution_setup.sh") {
          Some("distribution_setup.sh")
        } else {
          None
        }})) {
      return false
    }
    if (!ProcessUtils.runProcess("bash",
        Seq("./distribution_setup.sh", "developer", distributionServicePort.toString),
        Map.empty, distributionDir, Some(0), None, ProcessUtils.Logging.Realtime)) {
      log.error("Can't setup distribution server")
      return false
    }
    new File("distribution_setup.sh").delete()
    true
  }
}
