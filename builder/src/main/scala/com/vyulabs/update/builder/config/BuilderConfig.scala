package com.vyulabs.update.builder.config

import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId}
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.utils.JsonFormats._
import org.slf4j.Logger
import spray.json._

import java.io.File
import java.net.URL

case class DistributionLink(distributionName: DistributionName, distributionUrl: URL)

object DistributionLink extends DefaultJsonProtocol {
  implicit val distributionLinkJson = jsonFormat2(DistributionLink.apply)
}

case class BuilderConfig(instanceId: InstanceId, distributionLinks: Seq[DistributionLink])

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