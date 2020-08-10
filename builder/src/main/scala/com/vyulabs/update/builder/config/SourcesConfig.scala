package com.vyulabs.update.builder.config

import java.io.File
import java.net.URI

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json._

case class RepositoryConfig(uri: URI, cloneSubmodules: Boolean, directory: Option[String])

object RepositoryConfigJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URIJson._

  implicit val repositoryConfigJson = jsonFormat3(RepositoryConfig.apply)
}

case class SourcesConfig(sources: Map[ServiceName, Seq[RepositoryConfig]])

object SourcesConfigJson extends DefaultJsonProtocol {
  import RepositoryConfigJson._

  implicit val sourcesConfigListJson = jsonFormat1(SourcesConfig.apply)
}

object SourcesConfig {
  import SourcesConfigJson._

  def fromFile(directory: File)(implicit log: Logger): Option[SourcesConfig] = {
    val configFile = new File(directory, "sources.json")
    if (configFile.exists()) {
      IOUtils.readFileToJson(configFile).map(_.convertTo[SourcesConfig])
    } else {
      log.error(s"Config file ${configFile} does not exist")
      None
    }
  }
}