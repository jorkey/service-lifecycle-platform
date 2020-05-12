package com.vyulabs.update.config

import com.typesafe.config.Config
import com.vyulabs.update.common.Common.{InstallProfileName, ServiceName}
import scala.collection.JavaConverters._

case class ClientInstallProfile(profileName: InstallProfileName, serviceNames: Set[ServiceName])

object ClientInstallProfile {
  def apply(config: Config): ClientInstallProfile = {
    val profileName = config.getString("profile")
    val services = config.getStringList("services").asScala.toSet
    ClientInstallProfile(profileName, services)
  }
}

case class ClientInstallProfiles(profiles: Map[InstallProfileName, ClientInstallProfile])

object ClientInstallProfiles {
  def apply(config: Config): ClientInstallProfiles = {
    var profiles = Map.empty[InstallProfileName, ClientInstallProfile]
    config.getConfigList("profiles").asScala.foreach { config =>
      val profile = ClientInstallProfile(config)
      profiles += (profile.profileName -> profile)
    }
    ClientInstallProfiles(profiles)
  }
}
