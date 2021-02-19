package com.vyulabs.update.builder.config

import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json._

import java.io.File

case class RepositoryConfig(url: String, cloneSubmodules: Option[Boolean], directory: Option[String])

object RepositoryConfig extends DefaultJsonProtocol {
  implicit val repositoryConfigJson = jsonFormat3(RepositoryConfig.apply)
}

case class SourcesConfig(sources: Map[ServiceName, Seq[RepositoryConfig]])

object SourcesConfig extends DefaultJsonProtocol {
  import RepositoryConfig._

  implicit val sourcesConfigListJson = jsonFormat1(SourcesConfig.apply)

  def fromFile(file: File)(implicit log: Logger): Option[SourcesConfig] = {
    if (file.exists()) {
      IoUtils.readFileToJson[SourcesConfig](file)
    } else {
      log.error(s"Sources config file ${file} does not exist")
      None
    }
  }
}