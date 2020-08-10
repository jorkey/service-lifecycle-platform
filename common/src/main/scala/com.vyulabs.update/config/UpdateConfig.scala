package com.vyulabs.update.config

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.utils.IOUtils

import org.slf4j.Logger
import spray.json._

case class BuildConfig(buildCommands: Seq[CommandConfig], copyFiles: Seq[CopyFileConfig])
case class ServiceUpdateConfig(build: BuildConfig, install: Option[InstallConfig])
case class UpdateConfig(services: Map[ServiceName, ServiceUpdateConfig])

object UpdateConfigJson extends DefaultJsonProtocol {
  import InstallConfigJson._
  import CommandConfigJson._
  import CopyFileConfigJson._

  implicit val buildConfigJson = jsonFormat2(BuildConfig.apply)
  implicit val serviceUpdateConfigJson = jsonFormat2(ServiceUpdateConfig.apply)

  implicit object UpdateConfigFormat extends RootJsonFormat[UpdateConfig] {
    case class ServiceUpdateConfigJ(service: ServiceName, build: BuildConfig, install: Option[InstallConfig])
    case class UpdateConfigJ(update: Seq[ServiceUpdateConfigJ])

    implicit val serviceUpdateConfigJson = jsonFormat3(ServiceUpdateConfigJ.apply)
    implicit val updateConfigJson = jsonFormat1(UpdateConfigJ.apply)

    override def read(json: JsValue): UpdateConfig = {
      val services = json.convertTo[UpdateConfigJ].update
        .foldLeft(Map.empty[ServiceName, ServiceUpdateConfig])((m, v) => m + (v.service -> ServiceUpdateConfig(v.build, v.install)))
      UpdateConfig(services)
    }

    override def write(config: UpdateConfig): JsValue = {
      val update = config.services.foldLeft(Seq.empty[ServiceUpdateConfigJ])((m, v) => m :+ ServiceUpdateConfigJ(v._1, v._2.build, v._2.install))
      UpdateConfigJ(update).toJson
    }
  }
}

object UpdateConfig {
  def read(directory: File)(implicit log: Logger): Option[UpdateConfig] = {
    val configFile = new File(directory, Common.UpdateConfigFileName)
    if (configFile.exists()) {
      import UpdateConfigJson._
      IOUtils.readFileToJson(configFile).map(config => config.convertTo[UpdateConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}