package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.config._
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, ServicesProfileId, UserId}
import com.vyulabs.update.common.config.{DistributionConfig, GitConfig, SourceConfig}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries, administratorSubscriptions}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, InstallSettingsDirectory}
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.process.ProcessUtils
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{Build, ClientDistributionVersion, DeveloperDistributionVersion, DeveloperVersion}
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
class DistributionBuilder(cloudProvider: String, startService: () => Boolean,
                          distributionDirectory: DistributionDirectory,
                          distribution: String, distributionTitle: String,
                          mongoDbName: String, mongoDbTemporary: Boolean, port: Int)
                         (implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerBuilder = new DeveloperBuilder(distributionDirectory.getBuilderDir(), distribution)
  private val clientBuilder = new ClientBuilder(distributionDirectory.getBuilderDir(), distribution)

  private val initialClientVersion = ClientDistributionVersion.from(DeveloperDistributionVersion(distribution, Build.initialBuild), 0)

  private var adminDistributionClient = Option.empty[SyncDistributionClient[SyncSource]]
  private var developerDistributionClient = Option.empty[SyncDistributionClient[SyncSource]]
  private var builderDistributionClient = Option.empty[SyncDistributionClient[SyncSource]]

  private var distributionConfig = Option.empty[DistributionConfig]

  private var providerDistributionName = Option.empty[DistributionId]
  private var providerDistributionClient = Option.empty[SyncDistributionClient[SyncSource]]

  def buildDistributionFromSources(): Boolean = {
    log.info("")
    log.info(s"########################### Generate initial versions of services")
    log.info("")
    if (!generateDeveloperAndClientVersions(Map(
        (Common.DistributionServiceName -> DeveloperVersion(Build.initialBuild)),
        (Common.ScriptsServiceName -> DeveloperVersion(Build.initialBuild))))) {
      log.error("Can't generate initial versions")
      return false
    }

    log.info("")
    log.info(s"########################### Install distribution service")
    log.info("")
    if (!installDistributionService(initialClientVersion, initialClientVersion)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info("")
    log.info(s"########################### Distribution service is ready")
    log.info("")
    true
  }

  def addDistributionUsers(): Boolean = {
    log.info(s"--------------------------- Add distribution users")
    if (!addServiceUser(Common.InstallerServiceName, "Temporary install user", UserRole.Developer) ||
        !addServiceUser(Common.BuilderServiceName, "Builder service user", UserRole.Builder) ||
        !addServiceUser(Common.UpdaterServiceName, "Updater service user", UserRole.Updater)) {
      return false
    }
    true
  }

  def removeTemporaryDistributionUsers(): Boolean = {
    log.info(s"--------------------------- Remove temporary distribution users")
    if (!removeUser(Common.InstallerServiceName)) {
      return false
    }
    true
  }

  def buildFromProviderDistribution(providerDistributionName: DistributionId, providerDistributionURL: URL,
                                    servicesProfile: ServicesProfileId, testDistributionMatch: Option[String]): Boolean = {
    this.providerDistributionName = Some(providerDistributionName)
    providerDistributionClient = Some(new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(providerDistributionURL)), FiniteDuration(60, TimeUnit.SECONDS)))

    log.info("")
    log.info(s"########################### Download and generate client versions")
    log.info("")
    val scriptsVersion = downloadAndGenerateClientVersion(providerDistributionClient.get, Common.ScriptsServiceName).getOrElse {
      return false
    }
    val distributionVersion = downloadAndGenerateClientVersion(providerDistributionClient.get, Common.DistributionServiceName).getOrElse {
      return false
    }

    log.info("")
    log.info(s"########################### Install distribution service")
    log.info("")
    if (!installDistributionService(scriptsVersion, distributionVersion)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info("")
    log.info(s"########################### Add distribution provider to distribution server")
    log.info("")
    if (!adminDistributionClient.get.graphqlRequest(administratorMutations.addProvider(providerDistributionName, providerDistributionURL, None)).getOrElse(false)) {
      log.error(s"Can't add distribution provider")
      return false
    }

    log.info("")
    log.info(s"########################### Add distribution consumer to provider distribution server")
    log.info("")
    if (!providerDistributionClient.get.graphqlRequest(administratorMutations.addConsumer(distribution, servicesProfile, testDistributionMatch)).getOrElse(false)) {
      log.error(s"Can't add distribution consumer")
      return false
    }

    log.info("")
    log.info(s"########################### Distribution service is ready")
    log.info("")
    true
  }

  def addUpdateServicesSources(): Boolean = {
    val repository = GitRepository.openRepository(new File(".")).getOrElse(return false)
    val sourceConfig = SourceConfig("base", GitConfig(repository.getUrl(), repository.getBranch(), None))
    Seq(Common.ScriptsServiceName, Common.DistributionServiceName, Common.BuilderServiceName, Common.UpdaterServiceName)
      .foreach(service => {
        if (!adminDistributionClient.get.graphqlRequest(administratorMutations.addServiceSources(service, Seq(sourceConfig))).getOrElse(false)) {
          log.error(s"Can't add service ${service} sources")
          return false
        }
      })
    true
  }

  def generateAndUploadInitialVersions(author: String): Boolean = {
    log.info(s"--------------------------- Generate and upload initial versions")
    if (!uploadDeveloperAndClientVersions(Map(
      (Common.DistributionServiceName -> DeveloperDistributionVersion(distribution, Build.initialBuild)),
      (Common.ScriptsServiceName -> DeveloperDistributionVersion(distribution, Build.initialBuild))), author)) {
      return false
    }
    if (!generateAndUploadDeveloperAndClientVersions(Map(
      (Common.BuilderServiceName -> DeveloperVersion(Build.initialBuild)),
      (Common.UpdaterServiceName -> DeveloperVersion(Build.initialBuild))), author)) {
      return false
    }
    true
  }

  def addCommonServicesProfile(): Boolean = {
    log.info(s"--------------------------- Add common services profile")
    adminDistributionClient.get.graphqlRequest(
      administratorMutations.addServicesProfile(Common.CommonConsumerProfile, Seq(Common.DistributionServiceName,
        Common.ScriptsServiceName, Common.BuilderServiceName, Common.UpdaterServiceName))).getOrElse(false)
  }

  def updateDistributionFromProvider(): Boolean = {
    log.info(s"--------------------------- Get distribution provider desired versions")
    val providerDesiredVersions = DeveloperDesiredVersions.toMap(
        adminDistributionClient.get.graphqlRequest(administratorQueries.getDistributionProviderDesiredVersions(providerDistributionName.get)).getOrElse {
      log.error("Can't get provider distribution developer desired versions")
      return false
    })
    val versionsForUpdate = providerDesiredVersions.filter { case (service, version) =>
      val existingVersions = adminDistributionClient.get.graphqlRequest(administratorQueries.getDeveloperVersionsInfo(service)).getOrElse {
        log.error(s"Can't get distribution server existing versions of service ${service}")
        return false
      }
      !existingVersions.exists(_.version == version)
    }
    log.info(s"--------------------------- Versions for update ${versionsForUpdate}")
    versionsForUpdate.foreach { case (service, version) =>
      log.info(s"--------------------------- Install provider version ${version} of service ${service}")
      val taskId = adminDistributionClient.get.graphqlRequest(administratorMutations.installProviderVersion(providerDistributionName.get, service, version)).getOrElse {
        log.error(s"Can't install provider developer version ${version} of service ${service}")
        return false
      }
      val source = adminDistributionClient.get.graphqlSubRequest(administratorSubscriptions.subscribeTaskLogs(taskId)).getOrElse {
        log.error(s"Can't subscribe to task ${taskId} logs")
        return false
      }
      var line = Option.empty[SequencedServiceLogLine]
      do {
        line = source.next()
        line.foreach(line => {
          val l = line.logLine.line
          if (l.level == "INFO") {
            log.info(l.message)
          }
          for (terminationStatus <- l.terminationStatus) {
            if (!terminationStatus) {
              log.error(s"Install version ${version} of service ${service} error")
              return false
            }
          }
        })
      } while (line.isDefined)
    }
    log.info(s"--------------------------- Consumer distribution server is updated successfully")
    true
  }

  def waitForServerAvailable(waitingTimeoutSec: Int = 10000)
                            (implicit log: Logger): Boolean = {
    val client = adminDistributionClient.getOrElse {
      sys.error("No distribution client")
    }
    log.info(s"Wait for distribution server become available")
    for (_ <- 0 until waitingTimeoutSec) {
      if (client.available()) {
        Thread.sleep(1000)
        return true
      }
      Thread.sleep(1000)
    }
    log.error(s"Timeout of waiting for distribution server become available")
    false
  }

  def installBuilderFromSources(): Boolean = {
    log.info(s"--------------------------- Install builder")
    val updateSourcesUri = GitRepository.openRepository(new File(".")).map(_.getUrl())
    if (installBuilder(updateSourcesUri)) {
      log.info(s"--------------------------- Builder is installed successfully")
      true
    } else {
      false
    }
  }

  def installBuilder(updateSourcesUri: Option[String]): Boolean = {
    val config = distributionConfig.getOrElse {
      sys.error("No distribution config")
    }
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
    val distributionLinks = Seq(DistributionLink(distribution, makeDistributionUrl("builder")))
    if (!IoUtils.writeJsonToFile(new File(distributionDirectory.getBuilderDir(), Common.BuilderConfigFileName), BuilderConfig(config.instance, distributionLinks))) {
      return false
    }

    log.info(s"--------------------------- Create settings directory")
    val settingsDirectory = new InstallSettingsDirectory(distributionDirectory.getBuilderDir(), distribution)

//    for (updateSourcesUri <- updateSourcesUri) {
//      log.info(s"--------------------------- Create sources config")
//      val sourcesConfig = Map.empty[ServiceId, Seq[SourceConfig]] +
//        (Common.ScriptsServiceName -> Seq(SourceConfig("update", Some(GitConfig(updateSourcesUri, None)), None, None))) +
//        (Common.BuilderServiceName -> Seq(SourceConfig("update", Some(GitConfig(updateSourcesUri, None)), None, None))) +
//        (Common.UpdaterServiceName -> Seq(SourceConfig("update", Some(GitConfig(updateSourcesUri, None)), None, None))) +
//        (Common.DistributionServiceName -> Seq(SourceConfig("update", Some(GitConfig(updateSourcesUri, None)), None, None)))
//      if (!IoUtils.writeJsonToFile(settingsDirectory.getSourcesFile(), SourcesConfig(sourcesConfig))) {
//        log.error(s"Can't write sources config file")
//        return false
//      }
//    }
    true
  }

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersionDelta]): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.setDeveloperDesiredVersions(versions)).getOrElse(false)
  }

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersionDelta]): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.setClientDesiredVersions(versions)).getOrElse(false)
  }

  private def generateAndUploadDeveloperAndClientVersions(versions: Map[ServiceId, DeveloperVersion], author: String): Boolean = {
    generateDeveloperAndClientVersions(versions) &&
      uploadDeveloperAndClientVersions(versions.mapValues(v => DeveloperDistributionVersion.from(distribution, v)), author)
  }

  private def uploadDeveloperAndClientVersions(versions: Map[ServiceId, DeveloperDistributionVersion], author: String): Boolean = {
    if (!uploadDeveloperVersions(versions, author)) {
      return false
    }
    if (!uploadClientVersions(versions.mapValues(version => ClientDistributionVersion.from(version, 0)), author)) {
      return false
    }
    true
  }

  private def installDistributionService(scriptsVersion: ClientDistributionVersion, distributionVersion: ClientDistributionVersion): Boolean = {
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
    log.info(s"--------------------------- Make distribution config file")
    val arguments = Seq(cloudProvider, distribution, distributionTitle, mongoDbName, mongoDbTemporary.toString, port.toString)
    if (!ProcessUtils.runProcess("/bin/sh", ".make_distribution_config.sh" +: arguments, Map.empty,
      distributionDirectory.directory, Some(0), None, ProcessUtils.Logging.Realtime)) {
      log.error(s"Make distribution config file error")
      return false
    }
    log.info(s"--------------------------- Read distribution config")
    distributionConfig = DistributionConfig.readFromFile(distributionDirectory.getConfigFile())
    if (distributionConfig.isEmpty) {
      log.error(s"Can't read distribution config file ${distributionDirectory.getConfigFile()}")
      return false
    }

    adminDistributionClient = Some(new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(makeDistributionUrl("admin"))), FiniteDuration(60, TimeUnit.SECONDS)))
    log.info(s"--------------------------- Start distribution service")
    if (!startDistributionService()) {
      log.error("Can't start distribution service")
      return false
    }

    true
  }

  private def addServiceUser(user: UserId, name: String, role: UserRole): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.addUser(user, false,
        name, user, Seq(role))).getOrElse {
      return false
    }
    role match {
      case UserRole.Developer =>
        developerDistributionClient = Some(new SyncDistributionClient(
          new DistributionClient(new HttpClientImpl(makeDistributionUrl(user))), FiniteDuration(60, TimeUnit.SECONDS)))
      case UserRole.Builder =>
        builderDistributionClient = Some(new SyncDistributionClient(
          new DistributionClient(new HttpClientImpl(makeDistributionUrl(user))), FiniteDuration(60, TimeUnit.SECONDS)))
      case _ =>
    }
    true
  }

  private def removeUser(user: UserId): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.removeUser(user)).getOrElse {
      return false
    }
  }

  private def generateDeveloperAndClientVersions(versions: Map[ServiceId, DeveloperVersion]): Boolean = {
    versions.foreach { case (service, version) =>
      if (!generateDeveloperAndClientVersions(service, version)) {
        return false
      }
    }
    true
  }

  private def uploadDeveloperVersions(versions: Map[ServiceId, DeveloperDistributionVersion], author: String): Boolean = {
    log.info(s"--------------------------- Upload developer images of services ${versions.keySet}")
    versions.foreach { case (service, version) =>
      if (!developerBuilder.uploadDeveloperVersion(builderDistributionClient.get, service, version, author)) {
        log.error(s"Can't upload developer version ${version} of service ${service}")
        return false
      }
    }

    log.info(s"--------------------------- Set developer desired versions")
    if (!setDeveloperDesiredVersions(versions.map { case (service, version) =>
      DeveloperDesiredVersionDelta(service, Some(version)) }.toSeq)) {
      log.error("Set developer desired versions error")
      return false
    }
    true
  }

  private def uploadClientVersions(versions: Map[ServiceId, ClientDistributionVersion], author: String): Boolean = {
    log.info(s"--------------------------- Upload client images of services")
    versions.foreach { case (service, version) =>
      if (!clientBuilder.uploadClientVersion(builderDistributionClient.get, service, version, author)) {
        log.error(s"Can't upload developer version ${version} of service ${service}")
        return false
      }
    }

    log.info(s"--------------------------- Set client desired versions")
    if (!setClientDesiredVersions(versions.map { case (service, version) =>
        ClientDesiredVersionDelta(service, Some(version)) }.toSeq)) {
      log.error("Set developer desired versions error")
      return false
    }

    true
  }

  private def generateDeveloperAndClientVersions(service: ServiceId, developerVersion: DeveloperVersion): Boolean = {
    val developerDistributionVersion = DeveloperDistributionVersion.from(distribution, developerVersion)
    log.info(s"--------------------------- Generate version ${developerDistributionVersion} of service ${service}")
    log.info(s"Generate developer version of service ${service}")
    val arguments = Map.empty + ("version" -> developerDistributionVersion.toString)
    if (!developerBuilder.generateDeveloperVersion(service, new File("."), arguments)) {
      log.error(s"Can't generate developer version of service ${service}")
      return false
    }

    log.info(s"Copy developer version of service ${service} to client directory")
    if (!IoUtils.copyFile(developerBuilder.developerBuildDir(service), clientBuilder.clientBuildDir(service))) {
      log.error(s"Can't copy ${developerBuilder.developerBuildDir(service)} to ${clientBuilder.clientBuildDir(service)}")
      return false
    }

    log.info(s"Generate client version of service ${service}")
    if (!clientBuilder.generateClientVersion(service, Map.empty)) {
      log.error(s"Can't generate client version of service ${service}")
      return false
    }
    true
  }

  private def downloadAndGenerateClientVersion(developerDistributionClient: SyncDistributionClient[SyncSource],
                                               service: ServiceId): Option[ClientDistributionVersion] = {
    log.info(s"--------------------------- Get developer desired version of service ${service}")
    val desiredVersion = developerDistributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions(Seq(service))).getOrElse(Seq.empty).headOption.getOrElse {
      log.error(s"Can't get developer desired version of service ${service}")
      return None
    }
    val developerVersion = desiredVersion.version

    log.info(s"--------------------------- Download developer version of service ${service}")
    val developerVersionInfo = clientBuilder.downloadDeveloperVersion(developerDistributionClient,
        service, developerVersion).getOrElse {
      log.error("Can't download developer version of distribution service")
      return None
    }

    val clientVersion = ClientDistributionVersion(developerVersion.distribution, developerVersion.build, 0)
    log.info(s"--------------------------- Generate client version ${clientVersion} of service ${service}")
    if (!clientBuilder.generateClientVersion(service, Map.empty)) {
      log.error(s"Can't generate client version of service ${service}")
      return None
    }

    Some(clientVersion)
  }

  private def makeDistributionUrl(user: UserId): URL = {
    val config = distributionConfig.getOrElse {
      sys.error("No distribution config")
    }
    val protocol = if (config.network.ssl.isDefined) "https" else "http"
    val port = config.network.port
    new URL(s"${protocol}://${user}:${user}@localhost:${port}")
  }

  private def startDistributionService(): Boolean = {
    log.info(s"--------------------------- Start service")
    if (!startService()) {
      log.error("Can't start service process")
      return false
    }
    log.info(s"--------------------------- Waiting for distribution service became available")
    if (!waitForServerAvailable(10000)) {
      log.error("Can't start distribution server")
      return false
    }
    log.info("Distribution server is available")

    Thread.sleep(5000)
    true
  }
}