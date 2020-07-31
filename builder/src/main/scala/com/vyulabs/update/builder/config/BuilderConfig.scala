package com.vyulabs.update.builder.config

import java.io.File
import java.net.{URI, URL}

import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

case class BuilderConfig(adminRepositoryUri: URI, developerDistributionUrl: URL)

object BuilderConfig {
  def apply()(implicit log: Logger): Option[BuilderConfig] = {
    val configFile = new File("builder.json")
    if (configFile.exists()) {
      val config = IOUtils.parseConfigFile(configFile).getOrElse{ return None }
      val adminDirectoryUri = new URI(config.getString("adminRepositoryUrl"))
      val developerDistributionUrl = new URL(config.getString("developerDistributionUrl"))
      Some(BuilderConfig(adminDirectoryUri, developerDistributionUrl))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}