package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.ServiceId
import spray.json._

case class GitConfig(url: String, branch: Option[String], cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val gitConfigJson = jsonFormat3(GitConfig.apply)
}

case class SourceConfig(name: String, git: GitConfig)

object SourceConfig extends DefaultJsonProtocol {
  implicit val sourceConfigJson = jsonFormat2(SourceConfig.apply)
}

case class ServiceSourcesConfig(service: ServiceId, sources: Seq[SourceConfig])