package com.vyulabs.update.common.config

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId}
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol._

case class GitConfig(url: String, branch: String, cloneSubmodules: Option[Boolean])

object GitConfig extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(GitConfig.apply)
}

case class Repository(name: String, git: GitConfig)

object Repository extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(Repository.apply)
}

case class NameValue(name: String, value: String)

object NameValue extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(NameValue.apply)
}

case class ServiceConfig(service: ServiceId,
                         environment: Seq[NameValue],
                         repositories: Seq[Repository],
                         values: Seq[NameValue])

object ServiceConfig {
  implicit val json = jsonFormat4(ServiceConfig.apply)
}

case class BuilderConfig(distribution: DistributionId)

object BuilderConfig {
  implicit val json = jsonFormat1(BuilderConfig.apply)
}

