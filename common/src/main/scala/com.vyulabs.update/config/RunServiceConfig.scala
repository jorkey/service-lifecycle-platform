package com.vyulabs.update.config

import spray.json.DefaultJsonProtocol

case class RestartConditionsConfig(maxMemoryMB: Option[Long]) {
  def maxMemory = maxMemoryMB.map(_ * 1024 *1024)
}

case class RunServiceConfig(command: String, args: Seq[String], env: Option[Map[String, String]],
                            logWriter: LogWriterConfig, logUploader: Option[LogUploaderConfig], faultFilesMatch: Option[String],
                            restartOnFault: Boolean, restartConditions: Option[RestartConditionsConfig])

object RunServiceConfig extends DefaultJsonProtocol {
  import LogWriterConfig._
  import LogUploaderConfig._

  implicit val restartConditionsConfigJson = jsonFormat1(RestartConditionsConfig.apply)
  implicit val runServiceConfigJson = jsonFormat8(RunServiceConfig.apply)
}
