package com.vyulabs.update.builder

import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.utils.IoUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val distributionName = "test"
  val distributionDirectory = Files.createTempDirectory("distrib").toFile
  val settingsDir = Files.createTempDirectory("settings").toFile
  val settingsDirectory = new SettingsDirectory(settingsDir)
  val sourceBranch = "graphql"

  override protected def beforeAll() = {
    IoUtils.writeBytesToFile(settingsDirectory.getSourcesFile(),
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
    val builderDir = Files.createTempDirectory("builder").toFile
    val distributionBuilder = new DistributionBuilder(builderDir, Map("distribution_setup" -> "test_distribution_setup.sh"))
    assert(distributionBuilder.buildDistributionFromSources(distributionName, distributionDirectory, settingsDirectory, "test"))
  }
}
