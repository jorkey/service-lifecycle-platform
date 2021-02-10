package com.vyulabs.update.updater.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

case class UpdaterConfig(instanceId: InstanceId, clientDistributionUrl: URL)

object UpdaterConfig extends DefaultJsonProtocol {
  implicit val updaterConfigJson = jsonFormat2(UpdaterConfig.apply)

  def apply()(implicit log: Logger): Option[UpdaterConfig] = {
    val configFile = new File("updater.json")
    if (configFile.exists()) {
      IoUtils.readFileToJson[UpdaterConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
