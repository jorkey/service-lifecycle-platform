package com.vyulabs.update.config

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.logs.LogWriterInit

case class LogUploaderConfig(writer: LogWriterInit) {
  def toConfig(): Config = {
    val instance = ConfigFactory.empty()
      .withValue("writer", ConfigValueFactory.fromAnyRef(writer.toConfig()))
    ConfigFactory.empty().withValue("logUploader", instance.root())
  }
}

object LogUploaderConfig {
  def apply(config: Config): LogUploaderConfig = {
    val init = LogWriterInit(config.getConfig("writer"))
    LogUploaderConfig(init)
  }
}


