package com.vyulabs.update.config

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{ServiceName}

import scala.collection.JavaConverters._

case class InstallProfile(serviceNames: Set[ServiceName])

object InstallProfile {
  def apply(config: Config): InstallProfile = {
    val services = config.getStringList("services").asScala.toSet
    InstallProfile(services)
  }
}
