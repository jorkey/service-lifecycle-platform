package com.vyulabs.update.config

import java.io.File

import com.typesafe.config.Config
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import scala.collection.JavaConverters._

case class BuildConfig(BuildCommands: Seq[CommandConfig], CopyBuildFiles: Seq[CopyFileConfig])

object BuildConfig {
  def apply(config: Config): BuildConfig = {
    val buildCommands = if (config.hasPath("buildCommands")) config.getConfigList("buildCommands").asScala.map(CommandConfig(_)) else Seq.empty
    val copyBuildFiles = config.getConfigList("copyFiles").asScala.map(CopyFileConfig(_))
    new BuildConfig(buildCommands, copyBuildFiles)
  }
}

case class ServiceUpdateConfig(BuildConfig: BuildConfig,
                               InstallConfig: Option[InstallConfig])

object ServiceUpdateConfig {
  def apply(config: Config): ServiceUpdateConfig = {
    val buildConfig = BuildConfig(config.getConfig("build"))
    val installConfig = if (config.hasPath("install"))
      Some(InstallConfig(config.getConfig("install"))) else None
    ServiceUpdateConfig(buildConfig, installConfig)
  }
}

case class UpdateConfig(services: Map[ServiceName, ServiceUpdateConfig])

object UpdateConfig {
  def apply(directory: File)(implicit log: Logger): Option[UpdateConfig] = {
    val configFile = new File(directory, Common.UpdateConfigFileName)
    if (configFile.exists()) {
      val config = IOUtils.parseConfigFile(configFile).getOrElse(return None)
      val services = config.getConfigList("update").asScala.foldLeft(Map.empty[ServiceName, ServiceUpdateConfig]){
        case (map, config) => map + (config.getString("service") -> ServiceUpdateConfig(config))
      }
      new Some(UpdateConfig(services))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}