package com.vyulabs.update.config

import com.typesafe.config.Config
import scala.collection.JavaConverters._

case class CommandConfig(command: String, args: Seq[String], env: Map[String, String], directory: Option[String],
                         exitCode: Option[Int], outputMatch: Option[String])

object CommandConfig {
  def apply(config: Config): CommandConfig = {
    val command = config.getString("command")
    val args = if (config.hasPath("args")) config.getStringList("args").asScala else Seq.empty[String]
    val envConfig = if (config.hasPath("env")) Some(config.getConfig("env")) else None
    val env = envConfig.map(_.entrySet().asScala.foldLeft(Map.empty[String, String])((m, e) =>
      m + (e.getKey -> e.getValue.unwrapped().toString))).getOrElse(Map.empty)
    val directory = if (config.hasPath("directory")) Some(config.getString("directory")) else None
    val exitCode = if (config.hasPath("exitCode")) Some(config.getInt("exitCode")) else None
    val outputMatch = if (config.hasPath("outputMatch")) Some(config.getString("outputMatch")) else None
    CommandConfig(command, args, env, directory, exitCode, outputMatch)
  }
}
