package com.vyulabs.update.builder.config

import java.io.File
import java.net.{URI, URL}

import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json._

case class BuilderConfig(adminRepositoryUri: URI, developerDistributionUrl: URL)

object BuilderConfigJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URIJson._
  import com.vyulabs.update.utils.Utils.URLJson._

  implicit val builderConfigJson = jsonFormat2(BuilderConfig.apply)
}

object BuilderConfig {
  import BuilderConfigJson._

  def apply()(implicit log: Logger): Option[BuilderConfig] = {
    val configFile = new File("builder.json")
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(_.convertTo[BuilderConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}