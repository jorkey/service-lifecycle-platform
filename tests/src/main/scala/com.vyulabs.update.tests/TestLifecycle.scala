package com.vyulabs.update.tests

import com.vyulabs.libs.git.GitRepository
import com.vyulabs.update.builder.BuilderMain
import com.vyulabs.update.builder.config.{RepositoryConfig, SourcesConfig}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.{BuildConfig, InstallConfig, ServiceUpdateConfig, UpdateConfig}
import com.vyulabs.update.common.distribution.server.{DistributionDirectory, SettingsDirectory}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.LoggerFactory

import java.io.File
import java.net.URI
import java.nio.file.Files

class TestLifecycle {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def run(): Unit = {
    println("====================================== Setup and start distribution server")
    println()
    val distributionName = "test-distribution"
    val distributionDir = new DistributionDirectory(Files.createTempDirectory("distribution").toFile)
    BuilderMain.main(Array("buildDistribution", "cloudProvider=None",
      s"distributionDirectory=${distributionDir.directory.toString}", s"distributionName=${distributionName}", "distributionTitle=Test distribution server",
      "mongoDbName=test", "author=ak", "test=true"))

    println("====================================== Configure test service")
    println()
    val testServiceName = "test"
    val serviceSourceDir = Files.createTempDirectory("service").toFile
    val git = GitRepository.createBareRepository(serviceSourceDir).getOrElse {
      sys.error(s"Can't create Git repository in the file ${serviceSourceDir}")
    }
    val buildConfig = BuildConfig()
    val installConfig = InstallConfig()
    val updateConfig = UpdateConfig(Map.empty + (testServiceName -> ServiceUpdateConfig(buildConfig, Some(installConfig))))
    if (!IoUtils.writeJsonToFile(new File(serviceSourceDir, Common.UpdateConfigFileName), updateConfig)) {
      sys.error(s"Can't write update config file")
    }
    val settingsDirectory = new SettingsDirectory(distributionDir.directory, distributionName)
    val sourcesConfig = IoUtils.readFileToJson[SourcesConfig](settingsDirectory.getSourcesFile()).getOrElse {
      sys.error(s"Can't read sources config file")
    }
    sourcesConfig.sources + (testServiceName -> Seq(RepositoryConfig(new URI(git.getUrl()), None, None)))
    if (!IoUtils.writeJsonToFile(settingsDirectory.getSourcesFile(), sourcesConfig)) {
      sys.error(s"Can't write sources config file")
    }

    println("====================================== Build version of test service")
    println()

  }
}
