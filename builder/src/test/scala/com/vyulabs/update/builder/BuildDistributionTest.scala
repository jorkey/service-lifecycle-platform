package com.vyulabs.update.builder

import java.io.File

import com.vyulabs.update.common.distribution.server.DistributionDirectory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.file.Files

import com.vyulabs.update.common.process.{ChildProcess, ProcessUtils}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val developerDistributionName = "test-developer"
  val developerDistributionDir = Files.createTempDirectory("distrib").toFile

  val clientDistributionName = "test-client"
  val clientDistributionDir = Files.createTempDirectory("distrib").toFile

  val sourceBranch = "graphql"

  var processes = Set.empty[ChildProcess]

  override protected def afterAll(): Unit = {
    synchronized {
      processes.foreach(_.terminate())
    }
  }

  it should "build developer and client distribution" in {
    log.info(s"*************************** Build distribution from sources")
    log.info("")
    val developerDistributionBuilder = new DistributionBuilder(
      "None", () => startService(developerDistributionDir), new DistributionDirectory(developerDistributionDir),
      developerDistributionName, "Test developer distribution server",
      "BuildDistributionTest-developer",true, 8000)
    assert(developerDistributionBuilder.buildDistributionFromSources("ak"))

    log.info(s"*************************** Build client distribution from developer distribution")
    log.info("")
    val clientDistributionBuilder = new DistributionBuilder(
      "None", () => startService(clientDistributionDir), new DistributionDirectory(clientDistributionDir),
      clientDistributionName, "Test client distribution server",
      "BuildDistributionTest-client",true, 8001)
    assert(clientDistributionBuilder.buildFromDeveloperDistribution(new URL("http://admin:admin@localhost:8000"), "ak"))
  }

  def startService(directory: File): Boolean = {
    log.info("Start distribution server")
    val startProcess = ChildProcess.start("/bin/sh", Seq("distribution.sh"), Map.empty, directory)
    startProcess.onComplete {
      case Success(process) =>
        log.info("Distribution server started")
        synchronized {
          this.processes += process
        }
      case Failure(ex) =>
        sys.error("Can't start distribution process")
        ex.printStackTrace()
    }
    true
  }
}
