package com.vyulabs.update.config

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.utils.IoUtils
import org.slf4j.Logger

import spray.json._

case class InstallConfig(installCommands: Option[Seq[CommandConfig]], postInstallCommands: Option[Seq[CommandConfig]], runService: Option[RunServiceConfig])

object InstallConfig extends DefaultJsonProtocol {
  import CommandConfig._
  import RunServiceConfig._

  implicit val installConfigJson = jsonFormat3(InstallConfig.apply)

  def read(directory: File)(implicit log: Logger): Option[InstallConfig] = {
    val configFile = new File(directory, Common.InstallConfigFileName)
    if (configFile.exists()) {
      IoUtils.readFileToJson(configFile).map(config => config.convertTo[InstallConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}