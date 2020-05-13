package com.vyulabs.update.config

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.InstallProfileName

import scala.util.matching.Regex

case class ClientConfig(installProfileName: InstallProfileName, testClientMatch: Option[Regex]) {
  def toConfig(): Config = {
    var config = ConfigFactory.empty()
      .withValue("installProfile", ConfigValueFactory.fromAnyRef(installProfileName))
    for (testClientMatch <- testClientMatch) {
      config = config.withValue("testClientMatch", ConfigValueFactory.fromAnyRef(testClientMatch.toString()))
    }
    config
  }
}

object ClientConfig {
  def apply(config: Config): ClientConfig = {
    val profileName = config.getString("installProfile")
    val testClientMatch = if (config.hasPath("testClientMatch")) Some(config.getString("testClientMatch").r) else None
    ClientConfig(profileName, testClientMatch)
  }
}
