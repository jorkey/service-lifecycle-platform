package com.vyulabs.update.config

import com.typesafe.config.Config

import scala.collection.JavaConverters._

case class Setting(Name: String, Value: String)

object Setting {
  def apply(config: Config): Setting = {
    Setting(config.getString("name"), config.getString("value"))
  }
}

case class CopyFileConfig(SourceFile: String, DestinationFile: String, Except: Set[String], Settings: Map[String, String])

object CopyFileConfig {
  def apply(config: Config): CopyFileConfig = {
    val sourceFile = config.getString("sourceFile")
    val destinationFile = config.getString("destinationFile")
    val except = if (config.hasPath("except")) config.getStringList("except").asScala.toSet else Set.empty[String]
    val settings = if (config.hasPath("settings"))
      config.getConfigList("settings").asScala
        .map(Setting(_)).foldLeft(Map.empty[String, String])((map, entry) => map + (entry.Name -> entry.Value)) else Map.empty[String, String]
    CopyFileConfig(sourceFile, destinationFile, except, settings)
  }
}

