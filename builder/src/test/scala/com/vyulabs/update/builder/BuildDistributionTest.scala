package com.vyulabs.update.builder

import com.vyulabs.update.common.distribution.server.SettingsDirectory
import com.vyulabs.update.common.utils.IoUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global

class BuildDistributionTest extends FlatSpec with Matchers with BeforeAndAfterAll {
  behavior of "BuildDistribution"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  val distributionName = "test"
  val settingsDir = Files.createTempDirectory("settings").toFile
  val settingsDirectory = new SettingsDirectory(settingsDir)
  val sourceBranch = "graphql"

  override protected def beforeAll() = {
    println(s"${new File(".").getAbsolutePath} --- " + settingsDirectory.getSourcesFile())
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
    assert(DistributionBuilder.buildDistributionFromSources(distributionName, settingsDirectory, "graphql", "test"))
  }
}
