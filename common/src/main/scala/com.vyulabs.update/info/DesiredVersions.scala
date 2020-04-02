package com.vyulabs.update.info

import java.util.Date

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion

import scala.collection.JavaConverters._

case class DesiredVersions(Versions: Map[ServiceName, BuildVersion], TestRecords: Seq[TestRecord] = Seq.empty) {
  def toConfig(): Config = {
    val versions = Versions.foldLeft(ConfigFactory.empty())((config, entry) => {
      config.withValue(entry._1, ConfigValueFactory.fromAnyRef(entry._2.toString))
    })
    val config = ConfigFactory.empty()
      .withValue("desiredVersions", ConfigValueFactory.fromAnyRef(versions.root()))
    if (!TestRecords.isEmpty) {
      config.withValue("testRecords", ConfigValueFactory.fromIterable(TestRecords.map(_.toConfig()).asJava))
    }
    config
  }
}

object DesiredVersions {
  def apply(config: Config): DesiredVersions = {
    val versionsConfig = config.getConfig("desiredVersions")
    var versions = Map.empty[ServiceName, BuildVersion]
    for (version <- versionsConfig.entrySet().asScala) {
      versions += (version.getKey -> BuildVersion.parse(version.getValue.unwrapped().toString))
    }
    val testedRecords = (if (config.hasPath("testRecords")) config.getConfigList("testRecords").asScala else Seq.empty)
      .map(TestRecord(_))
    DesiredVersions(versions, testedRecords)
  }
}

case class TestRecord(ClientName: ClientName, Date: Date) {
  def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("clientName", ConfigValueFactory.fromAnyRef(ClientName))
      .withValue("date", ConfigValueFactory.fromAnyRef(Utils.serializeISO8601Date(Date)))
  }
}

object TestRecord {
  def apply(config: Config): TestRecord = {
    val clientName = config.getString("clientName")
    val date = Utils.parseISO8601Date(config.getString("date"))
    TestRecord(clientName, date)
  }
}