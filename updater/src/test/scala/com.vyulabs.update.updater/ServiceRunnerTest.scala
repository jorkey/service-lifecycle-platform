package com.vyulabs.update.updater

import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.config.{LogWriterConfig, RunServiceConfig}
import com.vyulabs.update.common.info.{FaultInfo, LogLine, ProfiledServiceName}
import com.vyulabs.update.common.logger.{LogReceiver, PrefixedLogger}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion, Build}
import com.vyulabs.update.updater.uploaders.FaultUploader
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.concurrent.{ExecutionContext, Future}

class ServiceRunnerTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })
  private implicit val timer = new ThreadTimer()
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  behavior of "Service runner tests"

  val service = ProfiledServiceName("test-service")

  val faultUploaderStub = new FaultUploader {
    override def addFaultReport(info: FaultInfo, reportFilesTmpDir: Option[File]): Unit = {}
    override def close(): Unit = {}
  }

  it should "start/stop service" in {
    val (stateController, serviceRunner) = makeServiceRunner("echo \"Script is executing\"\nsleep 10\n", None)

    serviceRunner.startService()
    assert(serviceRunner.isServiceRunning())

    assertResult(None)(stateController.getState().failuresCount)

    serviceRunner.stopService()
    assert(!serviceRunner.isServiceRunning())
  }

  it should "save logs of service" in {
    val (stateController, serviceRunner) = makeServiceRunner("echo \"2021-03-01 16:19:36.038 DEBUG module1 Script is executing\"\nsleep 10\n", None)

    serviceRunner.startService()
    assert(serviceRunner.isServiceRunning())

    Thread.sleep(1000)

    val logContent = new String(IoUtils.readFileToBytes(new File(stateController.logDirectory, "test.log")).getOrElse {
      sys.error("Can't read log file")
    }, "utf8")
    assertResult("2021-03-01 16:19:36.038 DEBUG module1 Script is executing\n")(logContent)

    serviceRunner.stopService()
    assert(!serviceRunner.isServiceRunning())
  }

  it should "upload logs of service" in {
    var logLines = Seq.empty[LogLine]

    val logUploaderStub = new LogReceiver {
      override def receiveLogLines(lines: Seq[LogLine]): Future[Unit] = {
        logLines ++= lines
        Future()
      }
    }

    val (stateController, serviceRunner) = makeServiceRunner("echo \"2021-03-01 16:19:36.038 DEBUG module1 Script is executing\"\n" +
      "echo \"Unformatted line\"\nsleep 10\n", Some(logUploaderStub))

    serviceRunner.startService()
    assert(serviceRunner.isServiceRunning())

    Thread.sleep(1000)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
    val logDate = dateFormat.parse("2021-03-01 16:19:36.038")

    assertResult(Seq(LogLine(logDate, "DEBUG", "module1", "Script is executing", None)))(
      logLines.filter(_.unit == "module1"))
    assertResult(Seq("`Service test-service` started", "Unformatted line"))(
      logLines.filter(_.unit == "SERVICE").map(_.message))

    serviceRunner.stopService()
    assert(!serviceRunner.isServiceRunning())
  }

  it should "save logs of failed service and restart it" in {
    val (stateController, serviceRunner) = makeServiceRunner("echo \"Script is executing\"", None)

    serviceRunner.startService()
    assert(serviceRunner.isServiceRunning())

    Thread.sleep(1000)

    assert(stateController.getState().failuresCount.getOrElse(0) > 0)
    assert(stateController.logHistoryDirectory.list().size > 0)

    serviceRunner.stopService()
    assert(!serviceRunner.isServiceRunning())
  }

  def makeServiceRunner(scriptContent: String, logUploader: Option[LogReceiver]): (ServiceStateController, ServiceRunner) = {
    val directory = Files.createTempDirectory("test").toFile
    val stateController = new ServiceStateController(directory, service, () => ())
    stateController.setVersion(ClientDistributionVersion.from(DeveloperDistributionVersion("test-distribution", Build.initialBuild), 0))

    val scriptFile = new File(stateController.currentServiceDirectory, "script.sh")
    if (!IoUtils.writeBytesToFile(scriptFile, scriptContent.getBytes("utf8"))) {
      sys.error(s"Can't write script")
    }
    if (!IoUtils.setExecuteFilePermissions(scriptFile)) {
      sys.error(s"Can't set execute permissions to script file")
    }

    val logWriter = LogWriterConfig("log", "test", 1, 10)
    val runServiceConfig = RunServiceConfig("/bin/sh", Some(Seq("-c", "./script.sh")),
      None, Some(logWriter), Some(logUploader.isDefined), Some("script.sh"), None, None)

    implicit val serviceLogger = new PrefixedLogger(s"Service ${service.toString}: ", log)
    val serviceRunner = new ServiceRunner(runServiceConfig, Map.empty, "none", service, stateController, logUploader, faultUploaderStub)

    (stateController, serviceRunner)
  }
}