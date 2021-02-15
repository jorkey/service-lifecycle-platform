package com.vyulabs.update.builder

import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.utils.IoUtils
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
  val developerBuilderDir = Files.createTempDirectory("builder").toFile
  val developerSettingsDirectory = new SettingsDirectory(developerBuilderDir, developerDistributionName)

  val clientDistributionName = "test-client"
  val clientDistributionDir = Files.createTempDirectory("distrib").toFile
  val clientBuilderDir = Files.createTempDirectory("builder").toFile
  val clientSettingsDirectory = new SettingsDirectory(clientBuilderDir, clientDistributionName)

  val sourceBranch = "graphql"

  override protected def beforeAll() = {
    IoUtils.writeBytesToFile(developerSettingsDirectory.getSourcesFile(),
      """{
      |  "sources": {
      |    "builder" : [
      |      {
      |        "url": "ssh://git@github.com/jorkey/update.git",
      |        "directory": "builder"
      |      }
      |    ],
      |    "scripts" : [
      |      {
      |        "url": "ssh://git@github.com/jorkey/update.git",
      |        "directory": "scripts"
      |      }
      |    ],
      |    "distribution" : [
      |      {
      |        "url": "ssh://git@github.com/jorkey/update.git",
      |        "directory": "distribution"
      |      }
      |    ]
      |  }
      |}""".stripMargin.getBytes("utf8"))
  }

  it should "build distribution from sources" in {
    val developerDistributionBuilder = new DistributionBuilder(developerBuilderDir,
      "None", false, developerDistributionDir, developerDistributionName, "Test developer distribution server",
      "BuildDistributionTest-developer",true, 8000)
    assert(developerDistributionBuilder.buildDistributionFromSources("ak"))

    val clientDistributionBuilder = new DistributionBuilder(clientBuilderDir,
      "None", false, clientDistributionDir, clientDistributionName, "Test client distribution server",
      "BuildDistributionTest-client",true, 8001)
    assert(clientDistributionBuilder.buildFromDeveloperDistribution(new URL("http://localhost:8000"), "ak"))
  }
}
