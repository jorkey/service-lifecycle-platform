package com.vyulabs.update.config

import spray.json.DefaultJsonProtocol

case class RestartConditionsConfig(maxMemoryMB: Option[Long]) {
  val maxMemory = maxMemoryMB.map(_ * 1024 *1024)
}

case class RunServiceConfig(command: String, args: Seq[String], env: Map[String, String],
                            logWriter: LogWriterConfig, logUploader: Option[LogUploaderConfig], faultFilesMatch: Option[String],
                            restartOnFault: Boolean, restartConditions: Option[RestartConditionsConfig])

object RunServiceConfigJson extends DefaultJsonProtocol {
  import LogWriterConfigJson._
  import LogUploaderConfigJson._

  implicit val restartConditionsConfigJson = jsonFormat1(RestartConditionsConfig)
  implicit val runServiceConfigJson = jsonFormat8(RunServiceConfig)
}
