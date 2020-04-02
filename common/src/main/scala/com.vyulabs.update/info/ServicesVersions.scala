package com.vyulabs.update.info

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion

import scala.collection.JavaConverters._

case class ServicesVersions(Versions: Map[ServiceName, BuildVersion]) {
  def toConfig(): Config = {
    val versions = Versions.foldLeft(ConfigFactory.empty())((config, entry) => {
      config.withValue(entry._1, ConfigValueFactory.fromAnyRef(entry._2.toString))
    })
    ConfigFactory.empty()
      .withValue("servicesVersions", ConfigValueFactory.fromAnyRef(versions.root()))
  }
}

object ServicesVersions {
  def apply(config: Config): ServicesVersions = {
    val versionsConfig = config.getConfig("servicesVersions")
    var versions = Map.empty[ServiceName, BuildVersion]
    for (version <- versionsConfig.entrySet().asScala) {
      versions += (version.getKey -> BuildVersion.parse(version.getValue.unwrapped().toString))
    }
    ServicesVersions(versions)
  }
}
