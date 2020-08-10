package com.vyulabs.update.config

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger

import spray.json._

case class InstallConfig(installCommands: Seq[CommandConfig], postInstallCommands: Seq[CommandConfig], runService: Option[RunServiceConfig])

object InstallConfigJson extends DefaultJsonProtocol {
  import CommandConfigJson._
  import RunServiceConfigJson._

  implicit val installConfigJson = jsonFormat3(InstallConfig.apply)
}

object InstallConfig {
  import InstallConfigJson._

  def apply(directory: File)(implicit log: Logger): Option[InstallConfig] = {
    val configFile = new File(directory, Common.InstallConfigFileName)
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(config => config.convertTo[InstallConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}