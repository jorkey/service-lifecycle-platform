package com.vyulabs.update.info

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion

import scala.collection.JavaConverters._

case class DesiredVersions(Versions: Map[ServiceName, BuildVersion], Tested: Boolean) {
  def toConfig(): Config = {
    val config = ConfigFactory.empty()
    val versions = Versions.foldLeft(ConfigFactory.empty())((config, entry) => {
      config.withValue(entry._1, ConfigValueFactory.fromAnyRef(entry._2.toString))
    })
    config.withValue("desiredVersions", ConfigValueFactory.fromAnyRef(versions.root()))
    config.withValue("tested", ConfigValueFactory.fromAnyRef(Tested))
  }
}

object DesiredVersions {
  def apply(config: Config): DesiredVersions = {
    val versionsConfig = config.getConfig("desiredVersions")
    var versions = Map.empty[ServiceName, BuildVersion]
    for (version <- versionsConfig.entrySet().asScala) {
      versions += (version.getKey -> BuildVersion.parse(version.getValue.unwrapped().toString))
    }
    val tested = if (config.hasPath("tested")) config.getBoolean("tested") else false
    DesiredVersions(versions, tested)
  }
}

