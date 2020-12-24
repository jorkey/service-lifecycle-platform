package com.vyulabs.update.builder

import com.vyulabs.update.builder.ClientBuilder._
import com.vyulabs.update.builder.DeveloperBuilder._
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, ServiceName}
import com.vyulabs.update.common.config.InstallConfig
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.SettingsDirectory
import org.slf4j.Logger

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
object DistributionBuilder {
  val distributionDirectory = new File("..")

  def buildDistributionFromSources(distributionName: DistributionName,
                                   settingsDirectory: SettingsDirectory, sourceBranch: String, author: String)
                                  (implicit executionContext: ExecutionContext): Boolean = {
    pullSourcesAndGenerateInitVersions(settingsDirectory,
      Seq(Common.ScriptsServiceName, Common.BuilderServiceName, Common.DistributionServiceName), Seq(sourceBranch),
      DeveloperDistributionVersion(distributionName, DeveloperVersion(Seq(1, 0, 0))))

    if (!installDistributionService(distributionDirectory)) {
      log.error("Can't install distribution service")
      return false
    }

    if (!startDistributionService(distributionDirectory)) {
      log.error("Can't start distribution service")
      return false
    }

    val distributionUrl: URL = null // TODO

    val distributionClient = new SyncDistributionClient(
      new DistributionClient(distributionName, new HttpClientImpl(distributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))
    if (!waitForServerAvailable(distributionClient, 10000)) {
      log.error("Can't start distribution service")
      return false
    }

    false
  }

  def buildFromDeveloperDistribution(developerDistributionName: DistributionName, distributionConfigFile: File): Boolean = {
    false
  }

  def installDistributionService(distributionDirectory: File): Boolean = {
    if (!IoUtils.copyFile(new File(clientBuildDir(Common.ScriptsServiceName), "distribution"), distributionDirectory) ||
      !IoUtils.copyFile(new File(clientBuildDir(Common.ScriptsServiceName), "update.sh"), distributionDirectory) ||
      !IoUtils.copyFile(clientBuildDir(Common.DistributionServiceName), distributionDirectory)) {
      return false
    }
    val installConfig = InstallConfig.read(distributionDirectory).getOrElse {
      log.error(s"No install config in the directory ${distributionDirectory}")
      return false
    }
    for (command <- installConfig.installCommands.getOrElse(Seq.empty)) {
      if (!ProcessUtils.runProcess(command, Map.empty, distributionDirectory, ProcessUtils.Logging.Realtime)) {
        log.error(s"Install distribution server error")
        return false
      }
    }
    true
  }

  def startDistributionService(distributionDirectory: File): Boolean = {
    val installConfig = InstallConfig.read(distributionDirectory).getOrElse {
      log.error(s"No install config in the directory ${distributionDirectory}")
      return false
    }
    for (run <- installConfig.runService) {
      log.info(s"Run distribution server")
      if (!ProcessUtils.runProcess(run.command, run.args.getOrElse(Seq.empty), run.env.getOrElse(Map.empty),
          distributionDirectory, Some(0), None, ProcessUtils.Logging.Realtime)) {
        log.error(s"Run distribution server error")
        return false
      }
    }
    true
  }

  def waitForServerAvailable(distributionClient: SyncDistributionClient[SyncSource], waitingTimeoutSec: Int = 10000)
                                  (implicit log: Logger): Boolean = {
    log.info(s"Wait for distribution server become available")
    for (_ <- 0 until waitingTimeoutSec) {
      if (distributionClient.available()) {
        return true
      }
      Thread.sleep(1000)
    }
    log.error(s"Timeout of waiting for distribution server become available")
    false
  }

  private def pullSourcesAndGenerateInitVersions(settingsDirectory: SettingsDirectory, serviceNames: Seq[ServiceName], sourceBranches: Seq[String],
                                                 version: DeveloperDistributionVersion): Boolean = {
    for (serviceName <- serviceNames) {
      log.info(s"Pull source repositories for service ${serviceName}")
      val sourceRepositories = pullSourceDirectories(settingsDirectory, serviceName, sourceBranches)
      if (sourceRepositories.isEmpty) {
        log.error(s"Can't pull source directories for service ${serviceName}")
        return false
      }

      log.info(s"Generate developer version ${version} for service ${serviceName}")
      if (!generateDeveloperVersion(serviceName, sourceRepositories.map(_.getDirectory()), Map.empty)) {
        log.error(s"Can't generate developer version")
        return false
      }

      log.info(s"Generate client version ${version} for service ${serviceName}")
      if (!generateClientVersion(settingsDirectory, serviceName, Map.empty)) {
        log.error(s"Can't generate client version")
        return false
      }
    }
    true
  }
}
