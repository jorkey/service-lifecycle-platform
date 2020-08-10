package com.vyulabs.update.updater.config

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class UpdaterConfig(instanceId: InstanceId, clientDistributionUrl: URL)

object UpdaterConfigJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val updaterConfigJson = jsonFormat2(UpdaterConfig.apply)
}

object UpdaterConfig {
  import UpdaterConfigJson._

  def apply()(implicit log: Logger): Option[UpdaterConfig] = {
    val configFile = new File("updater.json")
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(_.convertTo[UpdaterConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}
