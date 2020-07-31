package com.vyulabs.update.config

import java.io.File

import com.typesafe.config.Config
import com.vyulabs.update.common.Common
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import scala.collection.JavaConverters._

case class InstallConfig(Origin: Config, InstallCommands: Seq[CommandConfig], PostInstallCommands: Seq[CommandConfig], RunService: Option[RunServiceConfig])

object InstallConfig {
  def apply(config: Config): InstallConfig = {
    val installCommands = if (config.hasPath("installCommands"))
      config.getConfigList("installCommands").asScala.map(CommandConfig(_)) else Seq.empty
    val postInstallCommands = if (config.hasPath("postInstallCommands"))
      config.getConfigList("postInstallCommands").asScala.map(CommandConfig(_)) else Seq.empty
    val runServiceConfig = if (config.hasPath("runService"))
      Some(RunServiceConfig(config.getConfig("runService"))) else None
    InstallConfig(config, installCommands, postInstallCommands, runServiceConfig)
  }

  def apply(directory: File)(implicit log: Logger): Option[InstallConfig] = {
    val configFile = new File(directory, Common.InstallConfigFileName)
    if (configFile.exists()) {
      val config = IOUtils.parseConfigFile(configFile).getOrElse(return None)
      Some(InstallConfig(config))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}