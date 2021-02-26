package com.vyulabs.update.updater

import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.config.{LogUploaderConfig, LogWriterConfig, LogWriterInit, RunServiceConfig}
import com.vyulabs.update.common.info.{FaultInfo, ProfiledServiceName}
import com.vyulabs.update.common.logger.PrefixedLogger
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.updater.uploaders.FaultUploader
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
import scala.concurrent.ExecutionContext

class ServiceRunnerTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  private implicit val timer = new ThreadTimer()
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  behavior of "Service runner tests"

  val serviceName = ProfiledServiceName("test-service")

  val faultUploader = new FaultUploader {
    override def addFaultReport(info: FaultInfo, reportFilesTmpDir: Option[File]): Unit = {}
    override def close(): Unit = {}
  }

  it should "start/stop service" in {
    val directory = Files.createTempDirectory("test").toFile
    val stateController = new ServiceStateController(directory, serviceName, () => ())
    stateController.setVersion(ClientDistributionVersion(DeveloperDistributionVersion("test-distribution", DeveloperVersion.initialVersion)))

    val scriptFile = new File(stateController.currentServiceDirectory, "script.sh")
    val scriptContent = "echo \"Script is executing\""
    if (!IoUtils.writeBytesToFile(scriptFile, scriptContent.getBytes("utf8"))) {
      sys.error(s"Can't write script")
    }
    if (!IoUtils.setExecuteFilePermissions(scriptFile)) {
      sys.error(s"Can't set execute permissions to script file")
    }

    val logWriter = LogWriterConfig("log", "test", 1, 10, None)
    val logUploader = LogUploaderConfig(LogWriterInit("test", 1, 10))
    val runServiceConfig = RunServiceConfig("/bin/sh", Some(Seq("-c", "./script.sh")), None, Some(logWriter), Some(logUploader), None, None, None)

    implicit val serviceLogger = new PrefixedLogger(s"Service ${serviceName.toString}: ", log)
    val serviceRunner = new ServiceRunner(runServiceConfig, Map.empty, "none", serviceName, stateController, faultUploader)

    serviceRunner.startService()

    Thread.sleep(5000)

    serviceRunner.stopService()

    Thread.sleep(10000)
  }
}