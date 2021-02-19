package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.config.{BuilderConfig, DistributionLink, RepositoryConfig}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{InstanceId, ServiceName}
import com.vyulabs.update.common.config.{DistributionConfig, NetworkConfig, UploadStateConfig}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.administratorQueries
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, SettingsDirectory}
import com.vyulabs.update.common.info.ClientDesiredVersion
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class DistributionBuilder(cloudProvider: String, asService: Boolean,
                          distributionDirectory: DistributionDirectory, distributionName: String, distributionTitle: String,
                          mongoDbName: String, mongoDbTemporary: Boolean, port: Int)
                         (implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerBuilder = new DeveloperBuilder(distributionDirectory.getBuilderDir(), distributionName)
  private val clientBuilder = new ClientBuilder(distributionDirectory.getBuilderDir(), distributionName)

  def buildDistributionFromSources(author: String): Boolean = {
    log.info(s"########################### Generate initial versions of services")
    if (!generateDeveloperAndClientVersions(Common.ScriptsServiceName, DeveloperVersion.initialVersion) ||
        !generateDeveloperAndClientVersions(Common.BuilderServiceName, DeveloperVersion.initialVersion) ||
        !generateDeveloperAndClientVersions(Common.UpdaterServiceName, DeveloperVersion.initialVersion) ||
        !generateDeveloperAndClientVersions(Common.DistributionServiceName, DeveloperVersion.initialVersion)) {
      log.error("Can't generate initial versions")
      return false
    }

    log.info(s"########################### Install distribution service")
    val initialClientVersion = ClientDistributionVersion(DeveloperDistributionVersion(distributionName, DeveloperVersion.initialVersion))
    if (!installDistributionService(initialClientVersion, initialClientVersion)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info(s"########################### Read distribution config")
    val config = DistributionConfig.readFromFile(distributionDirectory.getConfigFile()).getOrElse {
      log.error(s"Can't read distribution config file ${distributionDirectory.getConfigFile()}")
      return false
    }
    val distributionUrl = makeDistributionUrl(config.network)

    log.info(s"########################### Start distribution service")
    val distributionClient = startDistributionService(distributionUrl).getOrElse {
      log.error("Can't start distribution service")
      return false
    }

    log.info(s"########################### Upload developer images of services")
    if (!developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.ScriptsServiceName, author) ||
        !developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.BuilderServiceName, author) ||
        !developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.UpdaterServiceName, author) ||
        !developerBuilder.uploadDeveloperInitVersion(distributionClient, Common.DistributionServiceName, author)) {
      log.error("Can't upload developer initial versions")
      return false
    }

    log.info(s"########################### Set developer desired versions")
    if (!developerBuilder.setInitialDesiredVersions(distributionClient, Seq(
        Common.ScriptsServiceName, Common.BuilderServiceName, Common.UpdaterServiceName, Common.DistributionServiceName))) {
      log.error("Set developer desired versions error")
      return false
    }

    log.info(s"########################### Upload client images of services")
    if (!clientBuilder.uploadClientVersion(distributionClient, Common.ScriptsServiceName, initialClientVersion, author) ||
        !clientBuilder.uploadClientVersion(distributionClient, Common.BuilderServiceName, initialClientVersion, author) ||
        !clientBuilder.uploadClientVersion(distributionClient, Common.DistributionServiceName, initialClientVersion, author)) {
      log.error("Can't upload client initial versions")
      return false
    }

    log.info(s"########################### Set client desired versions")
    if (!clientBuilder.setDesiredVersions(distributionClient, Seq(
          ClientDesiredVersion(Common.ScriptsServiceName, initialClientVersion),
          ClientDesiredVersion(Common.BuilderServiceName, initialClientVersion),
          ClientDesiredVersion(Common.UpdaterServiceName, initialClientVersion),
          ClientDesiredVersion(Common.DistributionServiceName, initialClientVersion)))) {
      log.error("Set client desired versions error")
      return false
    }

    log.info(s"########################### Install builder")
    val updateSourcesUri = GitRepository.openRepository(new File(".")).map(_.getUrl())
    if (!installBuilder(config.instanceId, Seq(DistributionLink(distributionName, distributionUrl)), updateSourcesUri)) {
      return false
    }

    log.info(s"########################### Distribution service is ready")
    true
  }

  def buildFromDeveloperDistribution(developerDistributionURL: URL, author: String): Boolean = {
    val developerDistributionClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(developerDistributionURL)), FiniteDuration(60, TimeUnit.SECONDS))

    log.info(s"########################### Download and generate client versions")
    val scriptsVersion = downloadDeveloperAndGenerateClientVersion(developerDistributionClient, Common.ScriptsServiceName).getOrElse {
      return false
    }
    val builderVersion = downloadDeveloperAndGenerateClientVersion(developerDistributionClient, Common.BuilderServiceName).getOrElse {
      return false
    }
    val updaterVersion = downloadDeveloperAndGenerateClientVersion(developerDistributionClient, Common.UpdaterServiceName).getOrElse {
      return false
    }
    val distributionVersion = downloadDeveloperAndGenerateClientVersion(developerDistributionClient, Common.DistributionServiceName).getOrElse {
      return false
    }

    log.info(s"########################### Install distribution service")
    if (!installDistributionService(scriptsVersion, distributionVersion)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info(s"########################### Read and modify distribution config")
    val config = DistributionConfig.readFromFile(distributionDirectory.getConfigFile()).getOrElse {
      log.error(s"Can't read distribution config file ${distributionDirectory.getConfigFile()}")
      return false
    }
    val distributionUrl = makeDistributionUrl(config.network)

    val uploadStateConfig = UploadStateConfig(developerDistributionURL, FiniteDuration(30, TimeUnit.SECONDS))
    val newDistributionConfig = DistributionConfig(config.name, config.title, config.instanceId, config.mongoDb, config.network,
      config.remoteBuilder, config.versions, config.instanceState, config.faultReports, Some(Seq(uploadStateConfig)))
    if (!IoUtils.writeJsonToFile(distributionDirectory.getConfigFile(), newDistributionConfig)) {
      log.error(s"Can't write distribution config file ${distributionDirectory.getConfigFile()}")
      return false
    }

    log.info(s"########################### Start distribution service")
    val distributionClient = startDistributionService(distributionUrl).getOrElse {
      log.error("Can't start distribution service")
      return false
    }

    log.info(s"########################### Upload client images of services")
    if (!clientBuilder.uploadClientVersion(distributionClient, Common.ScriptsServiceName, scriptsVersion, author) ||
        !clientBuilder.uploadClientVersion(distributionClient, Common.BuilderServiceName, builderVersion, author) ||
        !clientBuilder.uploadClientVersion(distributionClient, Common.UpdaterServiceName, updaterVersion, author) ||
        !clientBuilder.uploadClientVersion(distributionClient, Common.DistributionServiceName, distributionVersion, author)) {
      log.error("Can't upload client initial versions")
      return false
    }

    log.info(s"########################### Set client desired versions")
    if (!clientBuilder.setDesiredVersions(distributionClient, Seq(
        ClientDesiredVersion(Common.ScriptsServiceName, scriptsVersion),
        ClientDesiredVersion(Common.BuilderServiceName, builderVersion),
        ClientDesiredVersion(Common.UpdaterServiceName, updaterVersion),
        ClientDesiredVersion(Common.DistributionServiceName, distributionVersion)))) {
      log.error("Set client desired versions error")
      return false
    }

    log.info(s"########################### Install builder")
    if (!installBuilder(config.instanceId, Seq(DistributionLink(distributionName, distributionUrl)), None)) {
      return false
    }

    log.info(s"########################### Distribution service is ready")
    true
  }

  def installDistributionService(scriptsVersion: ClientDistributionVersion, distributionVersion: ClientDistributionVersion): Boolean = {
    if (!IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), "distribution"), distributionDirectory.directory) ||
        !IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), Common.UpdateSh), new File(distributionDirectory.directory, Common.UpdateSh)) ||
        !IoUtils.copyFile(clientBuilder.clientBuildDir(Common.DistributionServiceName), distributionDirectory.directory)) {
      return false
    }
    distributionDirectory.directory.listFiles().foreach { file =>
      if (file.getName.endsWith(".sh") && !IoUtils.setExecuteFilePermissions(file)) {
        return false
      }
    }
    if (!IoUtils.writeDesiredServiceVersion(distributionDirectory.directory, Common.ScriptsServiceName, scriptsVersion) ||
        !IoUtils.writeServiceVersion(distributionDirectory.directory, Common.ScriptsServiceName, scriptsVersion)) {
      return false
    }
    if (!IoUtils.writeDesiredServiceVersion(distributionDirectory.directory, Common.DistributionServiceName, distributionVersion) ||
        !IoUtils.writeServiceVersion(distributionDirectory.directory, Common.DistributionServiceName, distributionVersion)) {
      return false
    }
    log.info(s"Make distribution config file")
    val arguments = Seq(cloudProvider, distributionName, distributionTitle, mongoDbName, mongoDbTemporary.toString, port.toString)
    if (!ProcessUtils.runProcess("/bin/sh", "make_config.sh" +: arguments, Map.empty,
        distributionDirectory.directory, Some(0), None, ProcessUtils.Logging.Realtime)) {
      log.error(s"Make distribution config file error")
      return false
    }
    true
  }

  def makeDistributionUrl(networkConfig: NetworkConfig): URL = {
    val protocol = if (networkConfig.ssl.isDefined) "https" else "http"
    val port = networkConfig.port
    new URL(s"${protocol}://admin:admin@localhost:${port}")
  }

  def startDistributionService(distributionUrl: URL): Option[SyncDistributionClient[SyncSource]] = {
    log.info(s"--------------------------- Start distribution service")
    val startService = (script: String) => {
      ProcessUtils.runProcess("/bin/sh", script +: Seq.empty, Map.empty,
          distributionDirectory.directory, Some(0), None, ProcessUtils.Logging.Realtime)
    }
    if (asService) {
      if (!startService("create_service.sh")) {
        return None
      }
    } else {
      new Thread() {
        override def run(): Unit = {
          log.info("Start distribution server")
          while (true) {
            startService("distribution.sh")
            log.info("Distribution server is terminated. Restart distribution server")
          }
        }
      }.start()
    }
    log.info(s"--------------------------- Waiting for distribution service became available")
    log.info(s"Connect to distribution URL ${distributionUrl} ...")
    val distributionClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(distributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))
    if (!waitForServerAvailable(distributionClient, 10000)) {
      log.error("Can't start distribution server")
      return None
    }
    log.info("Distribution server is available")

    Some(distributionClient)
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

  def installBuilder(instanceId: InstanceId, distributionLinks: Seq[DistributionLink], updateSourcesUri: Option[String]): Boolean = {
    log.info(s"--------------------------- Initialize builder directory")
    if (!IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), "builder"), distributionDirectory.getBuilderDir()) ||
        !IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), Common.UpdateSh), new File(distributionDirectory.getBuilderDir(), Common.UpdateSh))) {
      return false
    }
    distributionDirectory.getBuilderDir().listFiles().foreach { file =>
      if (file.getName.endsWith(".sh") && !IoUtils.setExecuteFilePermissions(file)) {
        return false
      }
    }

    log.info(s"--------------------------- Create builder config")
    if (!IoUtils.writeJsonToFile(new File(distributionDirectory.getBuilderDir(), Common.BuilderConfigFileName), BuilderConfig(instanceId, distributionLinks))) {
      return false
    }

    log.info(s"--------------------------- Create settings directory")
    val settingsDirectory = new SettingsDirectory(distributionDirectory.getBuilderDir(), distributionName)

    for (updateSourcesUri <- updateSourcesUri) {
      log.info(s"--------------------------- Create sources config")
      val sourcesConfig = Map.empty[ServiceName, Seq[RepositoryConfig]] +
        (Common.ScriptsServiceName -> Seq(RepositoryConfig(updateSourcesUri, None, None))) +
        (Common.BuilderServiceName -> Seq(RepositoryConfig(updateSourcesUri, None, None))) +
        (Common.UpdaterServiceName -> Seq(RepositoryConfig(updateSourcesUri, None, None))) +
        (Common.DistributionServiceName -> Seq(RepositoryConfig(updateSourcesUri, None, None)))
      println(s"-------------- write config ${settingsDirectory.getSourcesFile()}")
      if (!IoUtils.writeJsonToFile(settingsDirectory.getSourcesFile(), sourcesConfig)) {
        log.error(s"Can't write sources config file")
        return false
      }
    }

    true
  }

  private def generateDeveloperAndClientVersions(serviceName: ServiceName, developerVersion: DeveloperVersion): Boolean = {
    val developerDistributionVersion = DeveloperDistributionVersion(distributionName, developerVersion)
    log.info(s"--------------------------- Generate version ${developerDistributionVersion} of service ${serviceName}")
    log.info(s"Generate developer version for service ${serviceName}")
    val arguments = Map.empty + ("version" -> developerDistributionVersion.toString)
    if (!developerBuilder.generateDeveloperVersion(serviceName, new File("."), arguments)) {
      log.error(s"Can't generate developer version for service ${serviceName}")
      return false
    }

    log.info(s"Copy developer initial version of service ${serviceName} to client directory")
    if (!IoUtils.copyFile(developerBuilder.developerBuildDir(serviceName), clientBuilder.clientBuildDir(serviceName))) {
      log.error(s"Can't copy ${developerBuilder.developerBuildDir(serviceName)} to ${clientBuilder.clientBuildDir(serviceName)}")
      return false
    }

    log.info(s"Generate client version for service ${serviceName}")
    if (!clientBuilder.generateClientVersion(serviceName, Map.empty)) {
      log.error(s"Can't generate client version for service ${serviceName}")
      return false
    }
    true
  }

  private def downloadDeveloperAndGenerateClientVersion(developerDistributionClient: SyncDistributionClient[SyncSource],
                                                        serviceName: ServiceName): Option[ClientDistributionVersion] = {
    log.info(s"--------------------------- Get developer desired version of service ${serviceName}")
    val desiredVersion = developerDistributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions(Seq(serviceName))).getOrElse(Seq.empty).headOption.getOrElse {
      log.error(s"Can't get developer desired version of service ${serviceName}")
      return None
    }
    val developerVersion = desiredVersion.version

    log.info(s"--------------------------- Download developer version of service ${serviceName}")
    val developerVersionInfo = clientBuilder.downloadDeveloperVersion(developerDistributionClient,
        serviceName, developerVersion).getOrElse {
      log.error("Can't download developer version of distribution service")
      return None
    }

    val clientVersion = ClientDistributionVersion(developerVersion.distributionName, ClientVersion(developerVersion.version))
    log.info(s"--------------------------- Generate client version ${clientVersion} for service ${serviceName}")
    if (!clientBuilder.generateClientVersion(serviceName, Map.empty)) {
      log.error(s"Can't generate client version for service ${serviceName}")
      return None
    }

    Some(clientVersion)
  }
}