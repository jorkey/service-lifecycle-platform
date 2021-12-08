package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val gitConfigJson = jsonFormat3(GitConfig.apply)
}

case class Source(name: String, git: GitConfig)

object Source extends DefaultJsonProtocol {
  implicit val sourceConfigJson = jsonFormat2(Source.apply)
}

case class ServiceSources(service: ServiceId, payload: Seq[Source])

object ServiceSources extends DefaultJsonProtocol {
  implicit val environmentVariableJson = jsonFormat2(ServiceSources.apply)
}

case class EnvironmentVariable(name: String, value: String)

object EnvironmentVariable extends DefaultJsonProtocol {
  implicit val environmentVariableJson = jsonFormat2(EnvironmentVariable.apply)
}

case class ServiceEnvironment(service: ServiceId, payload: Seq[EnvironmentVariable])

object ServiceEnvironment extends DefaultJsonProtocol {
  implicit val serviceEnvironmentJson = jsonFormat2(ServiceEnvironment.apply)
}

trait BuilderConfig {
  val distribution: DistributionId
  val environment: Seq[ServiceEnvironment]
}

case class DeveloperBuilderConfig(distribution: DistributionId,
                                  environment: Seq[ServiceEnvironment],
                                  sources: Seq[ServiceSources]) extends BuilderConfig

object DeveloperBuilderConfig {
  implicit val builderConfigJson = jsonFormat3(DeveloperBuilderConfig.apply)
}

case class ClientBuilderConfig(distribution: DistributionId,
                               environment: Seq[ServiceEnvironment]) extends BuilderConfig

object ClientBuilderConfig {
  implicit val builderConfigJson = jsonFormat2(ClientBuilderConfig.apply)
}
