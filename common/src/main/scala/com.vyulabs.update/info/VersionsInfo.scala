package com.vyulabs.update.info

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConverters._

case class VersionsInfo(info: Seq[VersionInfo]) {
  def toConfig(): Config = {
    val config = ConfigFactory.empty()
    val list = ConfigValueFactory.fromIterable(info.map(_.toConfig().root()).asJava)
    config.withValue("versions", list)
  }
}

object VersionsInfo {
  def apply(config: Config): VersionsInfo = {
    val info = config.getConfigList("versions").asScala.map(VersionInfo(_))
    VersionsInfo(info)
  }
}
