package com.vyulabs.update.info

import java.security.MessageDigest
import java.util.Date

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion

import scala.collection.JavaConverters._

case class DesiredVersions(Versions: Map[ServiceName, BuildVersion], TestSignatures: Seq[TestSignature] = Seq.empty) {
  def toConfig(): Config = {
    val versions = Versions.foldLeft(ConfigFactory.empty())((config, entry) => {
      config.withValue(entry._1, ConfigValueFactory.fromAnyRef(entry._2.toString))
    })
    var config = ConfigFactory.empty()
      .withValue("desiredVersions", ConfigValueFactory.fromAnyRef(versions.root()))
    val versionsString = Utils.renderConfig(versions.root().toConfig, true)
    val versionsHash = new String(MessageDigest.getInstance("SHA-1").digest(versionsString.getBytes("utf8").array))
    config = config.withValue("versionsHash", ConfigValueFactory.fromAnyRef(versionsHash))
    if (!TestSignatures.isEmpty) {
      config = config.withValue("testSignatures", ConfigValueFactory.fromIterable(TestSignatures.map(_.toConfig()).asJava))
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
    val testedRecords = (if (config.hasPath("testSignatures")) config.getConfigList("testSignatures").asScala else Seq.empty)
      .map(TestSignature(_))
    DesiredVersions(versions, testedRecords)
  }
}

case class TestSignature(ClientName: ClientName, Date: Date) {
  def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("clientName", ConfigValueFactory.fromAnyRef(ClientName))
      .withValue("date", ConfigValueFactory.fromAnyRef(Utils.serializeISO8601Date(Date)))
  }
}

object TestSignature {
  def apply(config: Config): TestSignature = {
    val clientName = config.getString("clientName")
    val date = Utils.parseISO8601Date(config.getString("date"))
    TestSignature(clientName, date)
  }
}