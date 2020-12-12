package com.vyulabs.update.builder.config

import java.io.File
import java.net.{URI, URL}
import com.vyulabs.update.common.Common.{DistributionName, InstanceId}
import com.vyulabs.update.utils.IoUtils
import com.vyulabs.update.utils.Utils.URLJson._
import com.vyulabs.update.utils.Utils.URIJson._
import org.slf4j.Logger
import spray.json._

case class DistributionLink(distributionName: DistributionName, distributionUrl: URL)

object DistributionLink extends DefaultJsonProtocol {
  implicit val distributionLinkJson = jsonFormat2(DistributionLink.apply)
}

case class BuilderConfig(instanceId: InstanceId, adminRepositoryUrl: URI, distributionLinks: Seq[DistributionLink])

object BuilderConfig extends DefaultJsonProtocol {
  implicit val builderConfigJson = jsonFormat3(BuilderConfig.apply)

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