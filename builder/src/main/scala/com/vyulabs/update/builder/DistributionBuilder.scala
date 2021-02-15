package com.vyulabs.update.builder

import com.vyulabs.update.builder.config.{BuilderConfig, DistributionLink}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId, ServiceName}
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class DistributionBuilder(builderDir: File, cloudProvider: String,
                          distributionDir: File, distributionName: String, distributionTitle: String, mongoDbName: String, test: Boolean) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerBuilder = new DeveloperBuilder(builderDir, distributionName)
  private val clientBuilder = new ClientBuilder(builderDir, distributionName)

  def buildDistributionFromSources(author: String)(implicit executionContext: ExecutionContext): Boolean = {
    log.info(s"------------------------------ Generate initial versions of services -----------------------------------")
    if (!generateInitVersions(Common.ScriptsServiceName) || !generateInitVersions(Common.BuilderServiceName) || !generateInitVersions(Common.DistributionServiceName)) {
      log.error("Can't generate init versions")
      return false
    }

    log.info(s"---------------------------------- Install distribution service ----------------------------------------")
    if (!installDistributionService()) {
      log.error("Can't install distribution service")
      return false
    }

    log.info(s"------------------------------------- Read distribution config -----------------------------------------")
    val config = DistributionConfig.readFromFile(new File(distributionDir, Common.DistributionConfigFileName)).getOrElse {
      log.error(s"Can't read distribution config file in the directory ${distributionDir}")
      return false
    }

    log.info(s"------------------------------------ Start distribution service ----------------------------------------")
    if (!startDistributionService()) {
      log.error("Can't start distribution service")
      return false
    }

    log.info(s"------------------------ Waiting for distribution service became available -----------------------------")
    val protocol = if (config.network.ssl.isDefined) "https" else "http"
    val port = config.network.port
    val distributionUrl = new URL(s"${protocol}://admin:admin@localhost:${port}")
    log.info(s"Connect to distribution URL ${distributionUrl} ...")

    val distributionClient = new SyncDistributionClient(
      new DistributionClient(distributionName, new HttpClientImpl(distributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))
    if (!waitForServerAvailable(distributionClient, 10000)) {
      log.error("Can't start distribution server")
      return false
    }
    log.info("Distribution server is available")

    log.info(s"-------------------------- Generate and upload developer images of services -----------------------------")
    if (!developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.ScriptsServiceName, author) ||
        !developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.BuilderServiceName, author) ||
        !developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.DistributionServiceName, author)) {
      log.error("Can't upload developer init versions")
      return false
    }

    log.info(s"----------------------------------- Set developer desired versions --------------------------------------")
    developerBuilder.setInitialDesiredVersions(distributionClient, Seq(
        Common.ScriptsServiceName, Common.BuilderServiceName, Common.DistributionServiceName))

    log.info(s"---------------------------- Generate and upload client images of services ------------------------------")
    if (!clientBuilder.uploadClientInitVersion(distributionClient, Common.ScriptsServiceName, author) ||
        !clientBuilder.uploadClientInitVersion(distributionClient, Common.BuilderServiceName, author) ||
        !clientBuilder.uploadClientInitVersion(distributionClient, Common.DistributionServiceName, author)) {
      log.error("Can't upload client init versions")
      return false
    }

    log.info(s"------------------------------------ Set client desired versions ----------------------------------------")
    clientBuilder.setInitialDesiredVersions(distributionClient, Seq(
      Common.ScriptsServiceName, Common.BuilderServiceName, Common.DistributionServiceName))

    log.info(s"------------------------------------------ Install builder ----------------------------------------------")
    installBuilder(config.instanceId, Seq(DistributionLink(distributionName, distributionUrl)))

    true
  }

  def buildFromDeveloperDistribution(developerDistributionName: DistributionName): Boolean = {
    false
  }

  def installDistributionService(): Boolean = {
    if (!IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), "distribution"), distributionDir) ||
        !IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), Common.UpdateSh), new File(distributionDir, Common.UpdateSh)) ||
        !IoUtils.copyFile(clientBuilder.clientBuildDir(Common.DistributionServiceName), distributionDir)) {
      return false
    }
    distributionDir.listFiles().foreach { file =>
      if (file.getName.endsWith(".sh") && !IoUtils.setExecuteFilePermissions(file)) {
        return false
      }
    }
    if (!IoUtils.writeDesiredServiceVersion(distributionDir, Common.ScriptsServiceName, clientBuilder.initialClientVersion) ||
        !IoUtils.writeServiceVersion(distributionDir, Common.ScriptsServiceName, clientBuilder.initialClientVersion)) {
      return false
    }
    if (!IoUtils.writeDesiredServiceVersion(distributionDir, Common.DistributionServiceName, clientBuilder.initialClientVersion) ||
        !IoUtils.writeServiceVersion(distributionDir, Common.DistributionServiceName, clientBuilder.initialClientVersion)) {
      return false
    }
    log.info(s"Make distribution config file")
    val arguments = Seq(cloudProvider, distributionName, distributionTitle, mongoDbName, false.toString)
    if (!ProcessUtils.runProcess("/bin/sh", "make_config.sh" +: arguments, Map.empty,
        distributionDir, Some(0), None, ProcessUtils.Logging.Realtime)) {
      log.error(s"Make distribution config file error")
      return false
    }
    true
  }

  def startDistributionService(): Boolean = {
    val startService = (script: String) => {
      if (!ProcessUtils.runProcess("/bin/sh", script +: Seq.empty, Map.empty,
          distributionDir, Some(0), None, ProcessUtils.Logging.Realtime)) {
        return false
      }
      true
    }
    if (!test) {
      startService("create_service.sh")
    } else {
      new Thread() {
        override def run(): Unit = {
          log.info("Start distribution server")
          startService("distribution.sh")
        }
      }.start()
      true
    }
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

  def installBuilder(instanceId: InstanceId, distributionLinks: Seq[DistributionLink]): Boolean = {
    log.info(s"--------------------------- Initialize builder directory")
    val builderDir = new File(distributionDir, "builder")
    if (!builderDir.mkdir()) {
      log.error(s"Can't make directory ${builderDir}")
      return false
    }
    if (!IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), "builder"), builderDir) ||
        !IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), Common.UpdateSh), new File(builderDir, Common.UpdateSh))) {
      return false
    }
    builderDir.listFiles().foreach { file =>
      if (file.getName.endsWith(".sh") && !IoUtils.setExecuteFilePermissions(file)) {
        return false
      }
    }

    log.info(s"--------------------------- Create builder config")
    if (!IoUtils.writeJsonToFile(new File(builderDir, Common.BuilderConfigFileName), BuilderConfig(instanceId, distributionLinks))) {
      return false
    }

    true
  }

  private def generateInitVersions(serviceName: ServiceName): Boolean = {
    log.info(s"--------------------------- Generate init version of service ${serviceName}")
    log.info(s"Generate developer version ${developerBuilder.initialDeveloperVersion} for service ${serviceName}")
    val arguments = Map.empty + ("version" -> developerBuilder.initialDeveloperVersion.toString)
    if (!developerBuilder.generateDeveloperVersion(serviceName, new File("."), arguments)) {
      log.error(s"Can't generate developer version for service ${serviceName}")
      return false
    }

    log.info(s"Copy developer init version of service ${serviceName} to client directory")
    if (!IoUtils.copyFile(developerBuilder.developerBuildDir(serviceName), clientBuilder.clientBuildDir(serviceName))) {
      log.error(s"Can't copy ${developerBuilder.developerBuildDir(serviceName)} to ${clientBuilder.clientBuildDir(serviceName)}")
      return false
    }

    log.info(s"Generate client version ${clientBuilder.initialClientVersion} for service ${serviceName}")
    if (!clientBuilder.generateClientVersion(serviceName, Map.empty)) {
      log.error(s"Can't generate client version for service ${serviceName}")
      return false
    }
    true
  }
}