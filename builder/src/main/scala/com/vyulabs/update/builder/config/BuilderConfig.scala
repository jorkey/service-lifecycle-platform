package com.vyulabs.update.builder.config

import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json._

import java.io.File

case class DistributionLink(distribution: DistributionId, distributionUrl: String)

object DistributionLink extends DefaultJsonProtocol {
  implicit val distributionLinkJson = jsonFormat2(DistributionLink.apply)
}

case class BuilderConfig(instance: InstanceId, distributionLinks: Seq[DistributionLink])

object BuilderConfig extends DefaultJsonProtocol {
  implicit val builderConfigJson = jsonFormat2(BuilderConfig.apply)

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