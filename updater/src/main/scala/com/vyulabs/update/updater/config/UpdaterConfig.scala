package com.vyulabs.update.updater.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId}
import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

case class UpdaterConfig(instanceId: InstanceId, clientDistributionUrl: URL)

object UpdaterConfig {
  def apply()(implicit log: Logger): Option[UpdaterConfig] = {
    val configFile = new File("updater.json")
    if (configFile.exists()) {
      val config = Utils.parseConfigFile(configFile).getOrElse{ return None }
      val instanceId = config.getString("instanceId")
      val clientDistributionUrl = new URL(config.getString("clientDistributionUrl"))
      Some(UpdaterConfig(instanceId, clientDistributionUrl))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
