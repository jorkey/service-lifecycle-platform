package com.vyulabs.update.installer.config

import java.io.File
import java.net.{URI, URL}

import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class InstallerConfig(adminRepositoryUrl: URI, developerDistributionUrl: URL, clientDistributionUrl: URL)

object InstallerConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URIJson._
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val builderConfigJson = jsonFormat3(InstallerConfig.apply)

  val configFile = new File("installer.json")

  def apply()(implicit log: Logger): Option[InstallerConfig] = {
    if (configFile.exists()) {
      IoUtils.readFileToJson[InstallerConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}