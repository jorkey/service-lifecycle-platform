package com.vyulabs.update.updater.config

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

import java.io.File

case class UpdaterConfig(instance: InstanceId, distributionUrl: String, accessToken: String)

object UpdaterConfig extends DefaultJsonProtocol {
  implicit val updaterConfigJson = jsonFormat3(UpdaterConfig.apply)

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
