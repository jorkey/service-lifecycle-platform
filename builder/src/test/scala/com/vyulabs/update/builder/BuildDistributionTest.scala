package com.vyulabs.update.builder

import com.vyulabs.update.common.distribution.server.DistributionDirectory
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.net.URL
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val developerDistributionName = "test-developer"
  val developerDistributionDir = Files.createTempDirectory("distrib").toFile

  val clientDistributionName = "test-client"
  val clientDistributionDir = Files.createTempDirectory("distrib").toFile

  val sourceBranch = "graphql"

  it should "build developer and client distribution" in {
    val developerDistributionBuilder = new DistributionBuilder(
      "None", false, new DistributionDirectory(developerDistributionDir),
      developerDistributionName, "Test developer distribution server",
      "BuildDistributionTest-developer",true, 8000)
    assert(developerDistributionBuilder.buildDistributionFromSources("ak"))

    val clientDistributionBuilder = new DistributionBuilder(
      "None", false, new DistributionDirectory(clientDistributionDir),
      clientDistributionName, "Test client distribution server",
      "BuildDistributionTest-client",true, 8001)
    assert(clientDistributionBuilder.buildFromDeveloperDistribution(new URL("http://admin:admin@localhost:8000"), "ak"))
  }
}
