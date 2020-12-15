package com.vyulabs.update.common.config

import java.io.File

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.utils.IoUtils
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
      IoUtils.readFileToJson[InstallConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}