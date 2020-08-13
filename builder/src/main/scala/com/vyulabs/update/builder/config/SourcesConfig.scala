package com.vyulabs.update.builder.config

import java.io.File
import java.net.URI

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.utils.IOUtils
import org.slf4j.Logger
import spray.json._

case class RepositoryConfig(uri: URI, cloneSubmodules: Option[Boolean], directory: Option[String])

object RepositoryConfig extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.URIJson._

  implicit val repositoryConfigJson = jsonFormat3(RepositoryConfig.apply)
}

case class SourcesConfig(sources: Map[ServiceName, Seq[RepositoryConfig]])

object SourcesConfig extends DefaultJsonProtocol {
  import RepositoryConfig._

  implicit val sourcesConfigListJson = jsonFormat1(SourcesConfig.apply)

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