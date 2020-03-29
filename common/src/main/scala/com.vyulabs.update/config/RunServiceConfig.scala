package com.vyulabs.update.config

import com.typesafe.config.Config
import com.vyulabs.update.logs.LogWriterInit

import scala.collection.JavaConverters._
import scala.util.matching.Regex

case class RestartConditionsConfig(MaxMemorySize: Option[Long])

object RestartConditionsConfig {
  def apply(config: Config): RestartConditionsConfig = {
    val maxMemorySize = if (config.hasPath("maxMemoryMB"))
      Some(config.getLong("maxMemoryMB") * 1024 * 1024) else None
    RestartConditionsConfig(maxMemorySize)
  }
}

case class RunServiceConfig(Command: String, Arguments: Seq[String], Env: Map[String, String],
                            LogWriter: LogWriterConfig, LogUploader: Option[LogUploaderConfig], FaultFilesMatch: Option[String],
                            RestartOnFault: Boolean, RestartConditions: Option[RestartConditionsConfig])

object RunServiceConfig {
  def apply(config: Config): RunServiceConfig = {
    val command = config.getString("command")
    val argsList = config.getStringList("args")
    val args = argsList.toArray(new Array[String](argsList.size())).toSeq
    val envConfig = if (config.hasPath("env")) Some(config.getConfig("env")) else None
    val env = envConfig.map(_.entrySet().asScala.foldLeft(Map.empty[String, String])((m, e) =>
      m + (e.getKey -> e.getValue.unwrapped().toString))).getOrElse(Map.empty)
    val logWriter = LogWriterConfig(config.getConfig("logWriter"))
    val logUploader = if (config.hasPath("logUploader")) Some(LogUploaderConfig(config.getConfig("logUploader"))) else None
    val faultFilesMatch =
      if (config.hasPath("faultFilesMatch")) Some(config.getString("faultFilesMatch"))
      else if (config.hasPath("coreFile")) Some(config.getString("coreFile")) // TODO remove after all are updated
      else Some("core")
    val restartOnFault = if (config.hasPath("restartOnFault")) config.getBoolean("restartOnFault") else true
    val restartConditions = if (config.hasPath("restartConditions"))
      Some(RestartConditionsConfig(config.getConfig("restartConditions"))) else None
    RunServiceConfig(command, args, env, logWriter, logUploader, faultFilesMatch, restartOnFault, restartConditions)
  }
}

