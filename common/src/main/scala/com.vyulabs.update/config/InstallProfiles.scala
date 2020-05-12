package com.vyulabs.update.config

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{InstallProfileName, ServiceName}
import scala.collection.JavaConverters._

case class InstallProfile(profileName: InstallProfileName, serviceNames: Set[ServiceName])

object InstallProfile {
  def apply(config: Config): InstallProfile = {
    val profileName = config.getString("profile")
    val services = config.getStringList("services").asScala.toSet
    InstallProfile(profileName, services)
  }
}

case class InstallProfiles(profiles: Map[InstallProfileName, InstallProfile])

object InstallProfiles {
  def apply(config: Config): InstallProfiles = {
    var profiles = Map.empty[InstallProfileName, InstallProfile]
    config.getConfigList("profiles").asScala.foreach { config =>
      val profile = InstallProfile(config)
      profiles += (profile.profileName -> profile)
    }
    InstallProfiles(profiles)
  }
}
