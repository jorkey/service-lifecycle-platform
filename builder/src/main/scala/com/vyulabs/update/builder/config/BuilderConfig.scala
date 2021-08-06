package com.vyulabs.update.builder.config

import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json._

import java.io.File

case class BuilderConfig(instance: InstanceId)

object BuilderConfig extends DefaultJsonProtocol {
  implicit val builderConfigJson = jsonFormat1(BuilderConfig.apply)

  def apply()(implicit log: Logger): Option[BuilderConfig] = {
    val configFile = new File("builder.json")
    if (configFile.exists()) {
      IoUtils.readFileToJson[BuilderConfig](configFile)
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}