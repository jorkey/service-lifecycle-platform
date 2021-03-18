package com.vyulabs.update.tests

import com.vyulabs.update.builder.config.{SourceConfig, SourcesConfig}
import com.vyulabs.update.builder.{ClientBuilder, DistributionBuilder}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.config._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorQueries, administratorSubscriptions}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, SettingsDirectory}
import com.vyulabs.update.common.info.{ClientDesiredVersionDelta, UserRole}
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.mongo.MongoDb
import com.vyulabs.update.updater.config.UpdaterConfig
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

class SimpleLifecycle {
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val distributionName = "test-distribution"
  private val distributionDir = Files.createTempDirectory("distribution").toFile
  private val builderDir = new File(distributionDir, "builder")
  private val settingsDirectory = new SettingsDirectory(builderDir, distributionName)
  private val adminDistributionUrl = new URL("http://admin:admin@localhost:8000")
  private val serviceDistributionUrl = new URL("http://service:service@localhost:8000")
  private val testServiceName = "test"
  private val testServiceSourcesDir = Files.createTempDirectory("service-sources").toFile
  private val testServiceInstanceDir = Files.createTempDirectory("service-instance").toFile

  private val distributionBuilder = new DistributionBuilder("None", startDistribution,
    new DistributionDirectory(distributionDir), distributionName, "Test distribution server", "test", false, 8000)
  private val clientBuilder = new ClientBuilder(builderDir, distributionName)

  private val distributionClient = new SyncDistributionClient(
    new DistributionClient(new HttpClientImpl(adminDistributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))

  private var processes = Set.empty[ChildProcess]

  private def startDistribution(): Boolean = {
    log.info("Start distribution server")
    val startProcess = ChildProcess.start("/bin/sh", Seq("distribution.sh"), Map.empty, distributionDir)
    startProcess.onComplete {
      case Success(process) =>
        log.info("Distribution server started")
        synchronized { processes += process }
        process.onTermination().map(_ => synchronized{ processes -= process })
      case Failure(ex) =>
        sys.error("Can't start distribution process")
        ex.printStackTrace()
    }
    true
  }

  Await.result(new MongoDb("test").dropDatabase(), FiniteDuration(10, TimeUnit.SECONDS))

  def close(): Unit = {
    synchronized {
      processes.foreach(_.terminate())
    }
  }

  def makeAndRunDistribution(): Unit = {
    if (!distributionBuilder.buildDistributionFromSources()) {
      sys.error("Can't build distribution server")
    }
  }

  def initializeDistribution(author: String): Unit = {
    println()
    println("########################### Initialize distribution")
    println()
    println("--------------------------- Add service user to distribution server")
    if (!distributionClient.graphqlRequest(administratorMutations.addUser("service", Seq(UserRole.Updater), "service")).getOrElse(false)) {
      sys.error("Can't initialize distribution")
    }

    if (!distributionBuilder.generateAndUploadInitialVersions(author) ||
        !distributionBuilder.addCommonConsumerProfile() ||
        !distributionBuilder.installBuilderFromSources()) {
      sys.error("Can't initialize distribution")
    }
    println()
    println(s"########################### Distribution server is initialized")
    println()
  }

  def installTestService(buggy: Boolean = false): Unit = {
    println()
    println(s"########################### Install test service")
    println()
    println(s"--------------------------- Configure test service in directory ${testServiceSourcesDir}")
    val buildConfig = BuildConfig(None, Seq(CopyFileConfig("sourceScript.sh", "runScript.sh", None, Some(Map.empty + ("version" -> "%%version%%")))))
    val installCommands = Seq(CommandConfig("chmod", Some(Seq("+x", "runScript.sh")), None, None, None, None))
    val logWriter = LogWriterConfig("log", "test", 1, 10)
    val installConfig = InstallConfig(Some(installCommands), None, Some(RunServiceConfig("/bin/sh", Some(Seq("-c", "./runScript.sh")),
      None, Some(logWriter), Some(false), None, None, None)))
    val updateConfig = UpdateConfig(Map.empty + (testServiceName -> ServiceUpdateConfig(buildConfig, Some(installConfig))))
    if (!IoUtils.writeJsonToFile(new File(testServiceSourcesDir, Common.UpdateConfigFileName), updateConfig)) {
      sys.error(s"Can't write update config file")
    }
    val scriptContent = "echo \"Executed version %%version%%\"" + (if (!buggy) "\nsleep 10000" else "")
    if (!IoUtils.writeBytesToFile(new File(testServiceSourcesDir, "sourceScript.sh"), scriptContent.getBytes("utf8"))) {
      sys.error(s"Can't write script")
    }
    val sourcesConfig = IoUtils.readFileToJson[SourcesConfig](settingsDirectory.getSourcesFile()).getOrElse {
      sys.error(s"Can't read sources config file")
    }
    val newSourcesConfig = SourcesConfig(sourcesConfig.sources + (testServiceName -> Seq(SourceConfig(Left(testServiceSourcesDir.getAbsolutePath), None))))
    if (!IoUtils.writeJsonToFile(settingsDirectory.getSourcesFile(), newSourcesConfig)) {
      sys.error(s"Can't write sources config file")
    }

    println(s"--------------------------- Make test service version")
    buildTestServiceVersions(distributionClient, DeveloperVersion.initialVersion)

    println(s"--------------------------- Setup and start updater with test service in directory ${testServiceInstanceDir}")
    if (!IoUtils.copyFile(new File("./scripts/updater/updater.sh"), new File(testServiceInstanceDir, "updater.sh")) ||
      !IoUtils.copyFile(new File("./scripts/.update.sh"), new File(testServiceInstanceDir, ".update.sh"))) {
      sys.error("Copying of updater scripts error")
    }
    val updaterConfig = UpdaterConfig("Test", serviceDistributionUrl)
    if (!IoUtils.writeJsonToFile(new File(testServiceInstanceDir, Common.UpdaterConfigFileName), updaterConfig)) {
      sys.error(s"Can't write ${Common.UpdaterConfigFileName}")
    }
    val process = Await.result(
      ChildProcess.start("/bin/sh", Seq("./updater.sh", "runServices", s"services=${testServiceName}"),
        Map.empty, testServiceInstanceDir, lines => lines.foreach(line => println(s"Updater: ${line._1}"))), FiniteDuration(15, TimeUnit.SECONDS))
    process.onTermination().map { status =>
      println(s"Updater is terminated with status ${status}")
      synchronized { processes -= process }
    }
    synchronized { processes += process }
    println()
    println(s"########################### Test service is installed")
    println()
  }

  def updateTestService(): Unit = {
    println()
    println(s"########################### Fix test service in directory ${testServiceInstanceDir}")
    println()
    val fixedScriptContent = "echo \"Executed version %%version%%\"\nsleep 10000\n"
    if (!IoUtils.writeBytesToFile(new File(testServiceSourcesDir, "sourceScript.sh"), fixedScriptContent.getBytes("utf8"))) {
      sys.error(s"Can't write script")
    }

    println(s"--------------------------- Make fixed test service version")
    buildTestServiceVersions(distributionClient, DeveloperVersion.initialVersion.next())

    println()
    println(s"########################### Test service is updated")
    println()
  }

  def updateDistribution(newVersion: ClientVersion): Unit = {
    println()
    println(s"########################### Upload new client version of distribution of version ${newVersion}")
    println()
    val newDistributionVersion = ClientDistributionVersion(distributionName, newVersion)
    if (!clientBuilder.uploadClientVersion(distributionClient, Common.DistributionServiceName, newDistributionVersion, "ak")) {
      sys.error(s"Can't write script")
    }
    if (!clientBuilder.setDesiredVersions(distributionClient, Seq(ClientDesiredVersionDelta(Common.DistributionServiceName, Some(newDistributionVersion))))) {
      sys.error("Set distribution desired version error")
    }

    println(s"--------------------------- Wait for distribution server updated")
    Thread.sleep(10000)
    distributionBuilder.waitForServerAvailable()
    Thread.sleep(5000)
    val states = distributionClient.graphqlRequest(administratorQueries.getServiceStates(distributionName = Some(distributionName),
      serviceName = Some(Common.DistributionServiceName))).getOrElse {
      sys.error("Can't get version of distribution server")
    }
    if (Some(newDistributionVersion) != states.head.instance.service.version) {
      sys.error(s"Distribution server version ${states.head.instance.service.version} is not equals expected ${newDistributionVersion}")
    }

    println()
    println(s"########################### Distribution is updated to version ${newVersion}")
    println()
  }

  private def buildTestServiceVersions(distributionClient: SyncDistributionClient[SyncSource], version: DeveloperVersion): Unit = {
    println("--------------------------- Build developer version of test service")
    val taskId = distributionClient.graphqlRequest(administratorMutations.buildDeveloperVersion(testServiceName, version)).getOrElse {
      sys.error("Can't execute build developer version task")
    }
    if (!subscribeTask(distributionClient, taskId)) {
      sys.error("Execution of build developer version task error")
    }

    println("--------------------------- Build client version of test service")
    val taskId1 = distributionClient.graphqlRequest(administratorMutations.buildClientVersion(testServiceName,
      DeveloperDistributionVersion(distributionName, version),
      ClientDistributionVersion(distributionName, ClientVersion(version)))).getOrElse {
      sys.error("Can't execute build client version task")
    }
    if (!subscribeTask(distributionClient, taskId1)) {
      sys.error("Execution of build client version task error")
    }

    println("--------------------------- Set client desired versions")
    if (!distributionClient.graphqlRequest(administratorMutations.setClientDesiredVersions(Seq(
        ClientDesiredVersionDelta(testServiceName, Some(ClientDistributionVersion(distributionName, ClientVersion(version))))))).getOrElse(false)) {
      sys.error("Set client desired versions error")
    }
  }

  private def subscribeTask(distributionClient: SyncDistributionClient[SyncSource], taskId: TaskId): Boolean = {
    val source = distributionClient.graphqlSubRequest(administratorSubscriptions.subscribeTaskLogs(taskId)).getOrElse {
      sys.error("Can't subscribe build developer version task")
    }
    while (true) {
      val log = source.next().getOrElse {
        sys.error("Unexpected end of subscription")
      }
      //println(log.logLine.line.message)
      for (terminationStatus <- log.logLine.line.terminationStatus) {
        println(s"Build developer version termination status is ${terminationStatus}")
        return terminationStatus
      }
    }
    false
  }
}
