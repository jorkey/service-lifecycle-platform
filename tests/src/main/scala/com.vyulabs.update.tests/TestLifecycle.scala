package com.vyulabs.update.tests

import com.vyulabs.update.builder.DistributionBuilder
import com.vyulabs.update.builder.config.{SourceConfig, SourcesConfig}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.config._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder.{administratorMutations, administratorSubscriptions}
import com.vyulabs.update.common.distribution.client.{DistributionClient, HttpClientImpl, SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, SettingsDirectory}
import com.vyulabs.update.common.info.{ClientDesiredVersion, UserRole}
import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.updater.config.UpdaterConfig
import org.slf4j.LoggerFactory
import spray.json.DefaultJsonProtocol._

import java.io.File
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

class TestLifecycle {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def run()(implicit executionContext: ExecutionContext): Unit = {
    val distributionName = "test-distribution"
    val distributionDir = Files.createTempDirectory("distribution").toFile
    val builderDir = new File(distributionDir, "builder")
    val settingsDirectory = new SettingsDirectory(builderDir, distributionName)
    val adminDistributionUrl = new URL("http://admin:admin@localhost:8000")
    val serviceDistributionUrl = new URL("http://service:service@localhost:8000")
    val testServiceName = "test"
    val testServiceSourcesDir = Files.createTempDirectory("service-sources").toFile
    val testServiceInstanceDir = Files.createTempDirectory("service-instance").toFile

    println()
    println("====================================== Setup and start distribution server")
    println()
    val distributionBuilder = new DistributionBuilder("None", false,
      new DistributionDirectory(distributionDir), distributionName, "Test distribution server", "test", true, 8000)
    if (!distributionBuilder.buildDistributionFromSources("ak")) {
      sys.error("Can't build distribution server")
    }
    val distributionClient = new SyncDistributionClient(
      new DistributionClient(new HttpClientImpl(adminDistributionUrl)), FiniteDuration(60, TimeUnit.SECONDS))

    println()
    println("====================================== Add service user to distribution server")
    println()
    if (!distributionClient.graphqlRequest(administratorMutations.addUser("service", UserRole.Service, "service")).getOrElse(false)) {
      sys.error("Can't add service user")
    }

    println()
    println(s"====================================== Configure test service in directory ${testServiceSourcesDir}")
    println()
    val buildConfig = BuildConfig(None, Seq(CopyFileConfig("sourceScript.sh", "runScript.sh", None, None)))
    val installConfig = InstallConfig(None, None, Some(RunServiceConfig("/bin/sh", Some(Seq("./runScript.sh", "|", "tee", "service.output")), None, None, None, None, None, None)))
    val updateConfig = UpdateConfig(Map.empty + (testServiceName -> ServiceUpdateConfig(buildConfig, Some(installConfig))))
    if (!IoUtils.writeJsonToFile(new File(testServiceSourcesDir, Common.UpdateConfigFileName), updateConfig)) {
      sys.error(s"Can't write update config file")
    }
    val scriptContent = "echo \"Executed version %%version%%\"\nsleep 10000"
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

    println()
    println("====================================== Build developer version of test service")
    println()
    val taskId = distributionClient.graphqlRequest(administratorMutations.buildDeveloperVersion(testServiceName, DeveloperVersion.initialVersion)).getOrElse {
      sys.error("Can't execute build developer version task")
    }
    if (!subscribeTask(distributionClient, taskId)) {
      sys.error("Execution of build developer version task error")
    }

    println()
    println("====================================== Build client version of test service")
    println()
    val taskId1 = distributionClient.graphqlRequest(administratorMutations.buildClientVersion(testServiceName,
        DeveloperDistributionVersion(distributionName, DeveloperVersion.initialVersion),
        ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion)))).getOrElse {
      sys.error("Can't execute build client version task")
    }
    if (!subscribeTask(distributionClient, taskId1)) {
      sys.error("Execution of build client version task error")
    }

    println()
    println("====================================== Set client desired versions")
    println()
    if (!distributionClient.graphqlRequest(administratorMutations.setClientDesiredVersions(Seq(
          ClientDesiredVersion(Common.DistributionServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion))),
          ClientDesiredVersion(Common.UpdaterServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion))),
          ClientDesiredVersion(Common.ScriptsServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion))),
          ClientDesiredVersion(Common.BuilderServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion))),
          ClientDesiredVersion(testServiceName, ClientDistributionVersion(distributionName, ClientVersion(DeveloperVersion.initialVersion)))))).getOrElse(false)) {
        log.error("Set client desired versions error")
      return false
    }

    println()
    println(s"====================================== Setup and start updater with test service in directory ${testServiceInstanceDir}")
    println()
    if (!IoUtils.copyFile(new File("./scripts/updater/updater.sh"), new File(testServiceInstanceDir, "updater.sh")) ||
        !IoUtils.copyFile(new File("./scripts/.update.sh"), new File(testServiceInstanceDir, ".update.sh"))) {
      sys.error("Copying of updater scripts error")
    }
    val updaterConfig = UpdaterConfig("Test", serviceDistributionUrl)
    if (!IoUtils.writeJsonToFile(new File(testServiceInstanceDir, Common.UpdaterConfigFileName), updaterConfig)) {
      sys.error(s"Can't write ${Common.UpdaterConfigFileName}")
    }
    val process = Await.result(
      ChildProcess.start("/bin/sh", Seq("./updater.sh", "runServices", s"services=${testServiceName}"), Map.empty, testServiceInstanceDir), FiniteDuration(15, TimeUnit.SECONDS))
    process.onTermination().map(status => println(s"Updater is terminated with status ${status}"))
    process.handleOutput(_.foreach(line => println(s"Updater: ${line}")))
    Thread.sleep(100000)
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
