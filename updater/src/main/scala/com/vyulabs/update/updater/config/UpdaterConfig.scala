package com.vyulabs.update.updater.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.utils.Utils
import org.slf4j.Logger

import scala.collection.JavaConverters._

case class UpdaterConfig(instanceId: InstanceId, servicesInstanceNames: Set[ServiceInstanceName], clientDistributionUrl: URL)

object UpdaterConfig {
  def apply()(implicit log: Logger): Option[UpdaterConfig] = {
    val configFile = new File("updater.json")
    if (configFile.exists()) {
      val config = Utils.parseConfigFile(configFile).getOrElse{ return None }
      val instanceId = config.getString("instanceId")
      val services = config.getStringList("services").asScala.toSet.map(ServiceInstanceName.parse(_))
      val clientDistributionUrl = new URL(config.getString("clientDistributionUrl"))
      Some(UpdaterConfig(instanceId, services, clientDistributionUrl))
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
