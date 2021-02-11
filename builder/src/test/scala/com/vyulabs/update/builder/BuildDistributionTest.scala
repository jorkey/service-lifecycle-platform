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
  val distributionDir = Files.createTempDirectory("distrib").toFile
  val builderDir = Files.createTempDirectory("builder").toFile
  val settingsDirectory = new SettingsDirectory(builderDir, distributionName)
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
    val distributionBuilder = new DistributionBuilder(builderDir, false,
      "None", distributionDir, distributionName, "Test distribution server", "test")
    assert(distributionBuilder.buildDistributionFromSources("ak"))
  }
}
