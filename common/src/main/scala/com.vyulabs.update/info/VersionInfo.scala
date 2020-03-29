package com.vyulabs.update.info

import java.util.Date

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.utils.UpdateUtils
import com.vyulabs.update.version.BuildVersion
import scala.collection.JavaConverters._

case class VersionInfo(buildVersion: BuildVersion, author: String, branches: Seq[String], date: Date, comment: Option[String]) {
  def toConfig(): Config = {
    var config = ConfigFactory.empty()
      .withValue("version", ConfigValueFactory.fromAnyRef(buildVersion.toString))
      .withValue("author", ConfigValueFactory.fromAnyRef(author))
      .withValue("branches", ConfigValueFactory.fromIterable(branches.asJava))
      .withValue("date", ConfigValueFactory.fromAnyRef(UpdateUtils.serializeISO8601Date(date)))
    for (comment <- comment) {
      config = config.withValue("comment", ConfigValueFactory.fromAnyRef(comment))
    }
    config
  }
}

object VersionInfo {
  def apply(config: Config): VersionInfo = {
    val version = BuildVersion.parse(config.getString("version"))
    val author = config.getString("author")
    val branches = if (config.hasPath("branches")) config.getStringList("branches").asScala else Seq.empty
    val date =
      if (config.hasPath("date")) {
        UpdateUtils.parseISO8601Date(config.getString("date"))
      } else {
        new Date(config.getLong("time"))
      }
    val comment = if (config.hasPath("comment")) Some(config.getString("comment")) else None
    VersionInfo(version, author, branches, date, comment)
  }
}
