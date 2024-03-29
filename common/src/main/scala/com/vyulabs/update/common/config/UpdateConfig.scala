package com.vyulabs.update.common.config

import java.io.File

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.ServiceId
import com.vyulabs.update.common.utils.IoUtils

import org.slf4j.Logger
import spray.json._

case class BuildConfig(buildCommands: Option[Seq[CommandConfig]], copyFiles: Seq[CopyFileConfig])
case class ServiceUpdateConfig(build: BuildConfig, install: Option[InstallConfig])
case class UpdateConfig(services: Map[ServiceId, ServiceUpdateConfig])

object UpdateConfig extends DefaultJsonProtocol {
  import InstallConfig._
  import CommandConfig._
  import CopyFileConfig._

  implicit val buildConfigJson = jsonFormat2(BuildConfig.apply)
  implicit val serviceUpdateConfigJson = jsonFormat2(ServiceUpdateConfig.apply)

  implicit object UpdateConfigFormat extends RootJsonFormat[UpdateConfig] {
    case class ServiceUpdateConfigJ(service: ServiceId, build: BuildConfig, install: Option[InstallConfig])
    case class UpdateConfigJ(update: Seq[ServiceUpdateConfigJ])

    implicit val serviceUpdateConfigJson = jsonFormat3(ServiceUpdateConfigJ.apply)
    implicit val updateConfigJson = jsonFormat1(UpdateConfigJ.apply)

    override def read(json: JsValue): UpdateConfig = {
      val services = json.convertTo[UpdateConfigJ].update
        .foldLeft(Map.empty[ServiceId, ServiceUpdateConfig])((m, v) => m + (v.service -> ServiceUpdateConfig(v.build, v.install)))
      UpdateConfig(services)
    }

    override def write(config: UpdateConfig): JsValue = {
      val update = config.services.foldLeft(Seq.empty[ServiceUpdateConfigJ])((m, v) => m :+ ServiceUpdateConfigJ(v._1, v._2.build, v._2.install))
      UpdateConfigJ(update).toJson
    }
  }

  def read(directory: File)(implicit log: Logger): Option[UpdateConfig] = {
    val configFile = new File(directory, Common.UpdateConfigFileName)
    if (configFile.exists()) {
      import UpdateConfig._
      IoUtils.readFileToJson[UpdateConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}