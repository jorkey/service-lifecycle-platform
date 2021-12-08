package com.vyulabs.update.builder

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.common.accounts.{ConsumerAccountProperties, UserAccountProperties}
import com.vyulabs.update.common.common.{Common}
import com.vyulabs.update.common.common.Common.{AccountId, DistributionId, ServiceId}
import com.vyulabs.update.common.config.{DistributionConfig, GitConfig, Source}
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries, administratorSubscriptions}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.process.{ChildProcess, ProcessUtils}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.{Build, ClientDistributionVersion, DeveloperDistributionVersion, DeveloperVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.02.19.
  * Copyright FanDate, Inc.
  */
class DistributionBuilder(cloudProvider: String, distribution: String,
                          directory: File, port: Int, title: String,
                          mongoDbConnection: String, mongoDbName: String, mongoDbTemporary: Boolean,
                          persistent: Boolean)
                         (implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val distributionDirectory = new DistributionDirectory(directory)

  private val developerBuilder = new DeveloperBuilder(distributionDirectory.getBuilderDir(distribution), distribution)
  private val clientBuilder = new ClientBuilder(distributionDirectory.getBuilderDir(distribution))

  private val initialClientVersion = ClientDistributionVersion.from(DeveloperDistributionVersion(distribution, Build.initialBuild), 0)

  private var adminDistributionClient = Option.empty[SyncDistributionClient[SyncSource]]
  private var adminProviderClient = Option.empty[SyncDistributionClient[SyncSource]]

  private var distributionConfig = Option.empty[DistributionConfig]

  private var providerDistributionName = Option.empty[DistributionId]

  def config = distributionConfig

  def buildDistributionFromSources(author: String): Boolean = {
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
    if (!installDistributionService(initialClientVersion, initialClientVersion, distribution)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info("")
    log.info(s"########################### Setup distribution service")
    log.info("")

    if (!addDistributionAccounts()) {
      log.error("Can't create distribution accounts")
      return false
    }

    if (!addUpdateServicesSources() ||
        !generateAndUploadInitialVersions(author) ||
        !addCommonServicesProfile() ||
        !addOwnServicesProfile()) {
      log.error("Can't initialize distribution")
      return false
    }

    log.info("")
    log.info(s"########################### Distribution service is ready")
    log.info("")

    true
  }

  def buildFromProviderDistribution(provider: DistributionId, providerURL: String, consumerAccessToken: String,
                                    testConsumer: Option[String], author: String): Boolean = {
    this.providerDistributionName = Some(provider)
    adminProviderClient = Some(new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(providerURL, Some(consumerAccessToken))),
        FiniteDuration(60, TimeUnit.SECONDS)))

    log.info("")
    log.info(s"########################### Download and generate client versions")
    log.info("")
    val scriptsVersion = downloadAndGenerateClientVersion(adminProviderClient.get, Common.ScriptsServiceName).getOrElse {
      return false
    }
    val distributionVersion = downloadAndGenerateClientVersion(adminProviderClient.get, Common.DistributionServiceName).getOrElse {
      return false
    }
    val builderVersion = downloadAndGenerateClientVersion(adminProviderClient.get, Common.BuilderServiceName).getOrElse {
      return false
    }
    val updaterVersion = downloadAndGenerateClientVersion(adminProviderClient.get, Common.UpdaterServiceName).getOrElse {
      return false
    }

    log.info("")
    log.info(s"########################### Install distribution service")
    log.info("")
    if (!installDistributionService(scriptsVersion, distributionVersion, provider)) {
      log.error("Can't install distribution service")
      return false
    }

    log.info("")
    log.info(s"########################### Upload versions")
    log.info("")
    if (!uploadDeveloperAndClientVersions(Map(
          Common.ScriptsServiceName -> scriptsVersion.original,
          Common.DistributionServiceName -> distributionVersion.original,
          Common.BuilderServiceName -> builderVersion.original,
          Common.UpdaterServiceName -> updaterVersion.original
        ), author)) {
      log.error("Can't upload versions")
      return false
    }

    log.info("")
    log.info(s"########################### Add distribution provider to distribution server")
    log.info("")
    if (!adminDistributionClient.get.graphqlRequest(administratorMutations.addProvider(provider,
        providerURL, consumerAccessToken, testConsumer, Some(true), None)).getOrElse(false)) {
      log.error(s"Can't add distribution provider")
      return false
    }

    log.info("")
    log.info(s"########################### Setup distribution service")
    log.info("")

    if (!addDistributionAccounts()) {
      log.error("Can't create distribution accounts")
      return false
    }

    log.info("")
    log.info(s"########################### Distribution service is ready")
    log.info("")

    true
  }

  def addDistributionAccounts(): Boolean = {
    log.info(s"--------------------------- Add distribution accounts")
    if (!addServiceAccount(Common.UpdaterServiceName, "Updater service account", AccountRole.Updater)) {
      return false
    }
    if (!addServiceAccount(Common.BuilderServiceName, "Builder service account", AccountRole.Builder)) {
      return false
    }
    true
  }

  def addUpdateServicesSources(): Boolean = {
    val repository = GitRepository.openRepository(new File(".")).getOrElse(return false)
    val sourceConfig = Source("base", GitConfig(repository.getUrl(), repository.getBranch(), None))
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

  def addOwnServicesProfile(): Boolean = {
    log.info(s"--------------------------- Add own services profile")
    adminDistributionClient.get.graphqlRequest(
      administratorMutations.addServicesProfile(Common.SelfConsumerProfile, Seq(Common.DistributionServiceName,
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
      log.info(s"--------------------------- Install version ${version} of service ${service}")
      val task = adminDistributionClient.get.graphqlRequest(administratorMutations.buildClientVersions(
          Seq(DeveloperDesiredVersion(service, version)))).getOrElse {
        log.error(s"Can't install provider developer version ${version} of service ${service}")
        return false
      }
      val source = adminDistributionClient.get.graphqlRequestSSE(administratorSubscriptions.subscribeTaskLogs(task)).getOrElse {
        log.error(s"Can't subscribe to task ${task} logs")
        return false
      }
      val lines = Option.empty[Seq[SequencedServiceLogLine]]
      do {
        for (lines <- source.next()) {
          lines.foreach(line => {
            val l = line.payload
            if (l.level == "INFO") {
              log.info(l.message)
            } else if (l.level == "WARN") {
              log.warn(l.message)
            } else if (l.level == "ERROR") {
              log.error(l.message)
            }
            for (terminationStatus <- l.terminationStatus) {
              if (!terminationStatus) {
                log.error(s"Install version ${version} of service ${service} error")
                return false
              }
            }
          })
        }
      } while (lines.isDefined)
    }
    log.info(s"--------------------------- Consumer distribution server is updated successfully")
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

  private def installDistributionService(scriptsVersion: ClientDistributionVersion,
                                         distributionVersion: ClientDistributionVersion,
                                         builderDistribution: DistributionId): Boolean = {
    if (!IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), "distribution"), directory) ||
      !IoUtils.copyFile(new File(clientBuilder.clientBuildDir(Common.ScriptsServiceName), Common.UpdateSh), new File(directory, Common.UpdateSh)) ||
      !IoUtils.copyFile(clientBuilder.clientBuildDir(Common.DistributionServiceName), directory)) {
      return false
    }
    directory.listFiles().foreach { file =>
      if (file.getName.endsWith(".sh") && !IoUtils.setExecuteFilePermissions(file)) {
        return false
      }
    }
    if (!IoUtils.writeDesiredServiceVersion(directory, Common.ScriptsServiceName, scriptsVersion) ||
      !IoUtils.writeServiceVersion(directory, Common.ScriptsServiceName, scriptsVersion)) {
      return false
    }
    if (!IoUtils.writeDesiredServiceVersion(directory, Common.DistributionServiceName, distributionVersion) ||
      !IoUtils.writeServiceVersion(directory, Common.DistributionServiceName, distributionVersion)) {
      return false
    }
    log.info(s"--------------------------- Make distribution config file")
    val arguments = Seq(cloudProvider, distribution, title, mongoDbConnection,
      mongoDbName, mongoDbTemporary.toString, port.toString, builderDistribution)
    if (!ProcessUtils.runProcess("/bin/bash", ".make_distribution_config.sh" +: arguments, Map.empty,
      directory, Some(0), None, ProcessUtils.Logging.Realtime)) {
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
      new DistributionClient(new HttpClientImpl(makeUrlWithCredentials("admin"))), FiniteDuration(60, TimeUnit.SECONDS)))

    startDistributionServer()
  }

  def addUserAccount(account: AccountId, name: String, role: AccountRole, password: String, properties: UserAccountProperties): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.addUserAccount(account, name, role, password, properties)).getOrElse {
      return false
    }
    true
  }

  def addServiceAccount(account: AccountId, name: String, role: AccountRole): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.addServiceAccount(account, name, role)).getOrElse {
      return false
    }
    true
  }

  def addConsumerAccount(distribution: AccountId, name: String, consumer: ConsumerAccountProperties): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.addConsumerAccount(distribution,
        name, consumer)).getOrElse {
      return false
    }
    true
  }

  private def removeAccount(user: AccountId): Boolean = {
    adminDistributionClient.get.graphqlRequest(administratorMutations.removeAccount(user)).getOrElse {
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
      if (!developerBuilder.uploadDeveloperVersion(adminDistributionClient.get, service, version, author)) {
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
      if (!clientBuilder.uploadClientVersion(adminDistributionClient.get, service, version, author)) {
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
    val desiredVersion = developerDistributionClient.graphqlRequest(administratorQueries.getDeveloperDesiredVersions(Seq(service)))
        .getOrElse(Seq.empty).headOption.getOrElse {
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

  private def makeUrlWithCredentials(user: AccountId): String = {
    val config = distributionConfig.getOrElse {
      sys.error("No distribution config")
    }
    val protocol = if (config.network.ssl.isDefined) "https" else "http"
    val port = config.network.port
    Utils.addCredentialsToUrl(s"${protocol}://localhost:${port}", user, user)
  }

  private def startDistributionServer(): Boolean = {
    if (persistent) {
      log.info("--------------------------- Install and start distribution service")
      ProcessUtils.runProcess("/bin/bash", Seq(".create_distribution_service.sh"), Map.empty,
        directory, Some(0), None, ProcessUtils.Logging.Realtime)
    } else {
      log.info("--------------------------- Start distribution server")
      val startProcess = ChildProcess.start("/bin/bash", Seq("distribution.sh"), Map.empty,
        directory)
      startProcess.onComplete {
        case Success(process) =>
          log.info("Distribution server started")
        case Failure(ex) =>
          sys.error("Can't start distribution process")
          ex.printStackTrace()
      }
    }
    if (!waitForServerAvailable(10000)) {
      return false
    }

    Thread.sleep(5000)
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
        log.info("Distribution server is available")
        Thread.sleep(5000)
        return true
      }
      Thread.sleep(1000)
    }
    log.error(s"Timeout of waiting for distribution server become available")
    false
  }
}