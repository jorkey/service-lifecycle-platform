package com.vyulabs.update.builder.config

import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.utils.IoUtils
import org.slf4j.Logger
import spray.json._

import java.io.File

case class GitConfig(url: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val gitConfigJson = jsonFormat2(GitConfig.apply)
}

case class SourceConfig(subDirectory: Option[String], git: Option[GitConfig])

object SourceConfig extends DefaultJsonProtocol {
  implicit val repositoryConfigJson = jsonFormat2(SourceConfig.apply)
}

case class SourcesConfig(sources: Map[ServiceName, Seq[SourceConfig]])

object SourcesConfig extends DefaultJsonProtocol {
  import SourceConfig._

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