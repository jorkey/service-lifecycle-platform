package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

case class BuilderConfig(distribution: DistributionId)

object BuilderConfig {
  implicit val json = jsonFormat1(BuilderConfig.apply)
}

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(GitConfig.apply)
}

case class Repository(name: String, git: GitConfig)

object Repository extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(Repository.apply)
}

case class NamedStringValue(name: String, value: String)

object NamedStringValue extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(NamedStringValue.apply)
}

case class ServiceConfig(service: ServiceId,
                         distribution: Option[DistributionId],
                         environment: Seq[NamedStringValue],
                         repositories: Seq[Repository],
                         macroValues: Seq[NamedStringValue])

object ServiceConfig {
  implicit val json = jsonFormat5(ServiceConfig.apply)
}