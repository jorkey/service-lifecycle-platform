package com.vyulabs.update.installer.config

import java.io.File
import java.net.{URI, URL}

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.ClientName
import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

import scala.util.matching.Regex

case class InstallerConfig(clientName: ClientName, adminRepositoryUri: URI, developerDistributionUrl: URL, clientDistributionUrl: URL) {
  def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("client", ConfigValueFactory.fromAnyRef(clientName))
      .withValue("adminRepositoryUri", ConfigValueFactory.fromAnyRef(adminRepositoryUri.toString))
      .withValue("developerDistributionUrl", ConfigValueFactory.fromAnyRef(developerDistributionUrl.toString))
      .withValue("clientDistributionUrl", ConfigValueFactory.fromAnyRef(clientDistributionUrl.toString))
  }

  def write()(implicit log: Logger): Boolean = {
    Utils.writeConfigFile(InstallerConfig.configFile, toConfig())
  }
}

object InstallerConfig {
  val configFile = new File("installer.json")

  def apply()(implicit log: Logger): Option[InstallerConfig] = {
    if (configFile.exists()) {
      val config = Utils.parseConfigFile(configFile).getOrElse{ return None }
      val clientName = config.getString("client")
      val adminRepositoryUri = new URI(config.getString("adminRepositoryUrl"))
      val developerDistributionUrl = new URL(config.getString("developerDistributionUrl"))
      val clientDistributionUrl = new URL(config.getString("clientDistributionUrl"))
      Some(InstallerConfig(clientName, adminRepositoryUri, developerDistributionUrl, clientDistributionUrl))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}