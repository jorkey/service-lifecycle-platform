package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(GitConfig.apply)
}

case class Source(name: String, git: GitConfig)

object Source extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(Source.apply)
}

case class EnvironmentVariable(name: String, value: String)

object EnvironmentVariable extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(EnvironmentVariable.apply)
}

trait ServiceConfig {
  val environment: Seq[EnvironmentVariable]
}

case class DeveloperServiceConfig(service: ServiceId,
                                  environment: Seq[EnvironmentVariable],
                                  sources: Seq[Source]) extends ServiceConfig

object DeveloperServiceConfig {
  implicit val json = jsonFormat3(DeveloperServiceConfig.apply)
}


case class ClientServiceConfig(service: ServiceId,
                               environment: Seq[EnvironmentVariable]) extends ServiceConfig

object ClientServiceConfig {
  implicit val json = jsonFormat2(ClientServiceConfig.apply)
}

case class BuilderConfig(distribution: DistributionId)

object BuilderConfig {
  implicit val json = jsonFormat1(BuilderConfig.apply)
}

