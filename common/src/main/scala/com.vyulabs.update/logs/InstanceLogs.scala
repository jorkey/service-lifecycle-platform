package com.vyulabs.update.logs

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}

import scala.collection.JavaConverters._
import scala.collection._

case class LogWriterInit(FilePrefix: String, MaxFileSizeMB: Int, MaxFilesCount: Int) {
  def toConfig(): Config = {
    ConfigFactory.empty()
      .withValue("filePrefix", ConfigValueFactory.fromAnyRef(FilePrefix))
      .withValue("maxFileSizeMB", ConfigValueFactory.fromAnyRef(MaxFileSizeMB))
      .withValue("maxFilesCount", ConfigValueFactory.fromAnyRef(MaxFilesCount))
  }
}

object LogWriterInit {
  def apply(config: Config): LogWriterInit = {
    val filePrefix = config.getString("filePrefix")
    val maxFileSizeMB = config.getInt("maxFileSizeMB")
    val maxFilesCount = config.getInt("maxFilesCount")
    new LogWriterInit(filePrefix, maxFileSizeMB, maxFilesCount)
  }
}

case class ServiceLogs(writerInit: Option[LogWriterInit], records: Seq[String]) {
  def toConfig(): Config = {
    var config = ConfigFactory.empty()
    for (writerInit <- writerInit) {
      config = config.withValue("writerInit", ConfigValueFactory.fromAnyRef(writerInit.toConfig().root()))
    }
    config = config.withValue("records", ConfigValueFactory.fromIterable(records.asJava))
    config
  }
}

object ServiceLogs {
  def apply(config: Config): ServiceLogs = {
    val writerInit =
      if (config.hasPath("writerInit")) {
        val writerConfig = config.getConfig("writerInit")
        Some(LogWriterInit(writerConfig))
      } else {
        None
      }
    val records = config.getStringList("records").asScala
    new ServiceLogs(writerInit, records)
  }
}
