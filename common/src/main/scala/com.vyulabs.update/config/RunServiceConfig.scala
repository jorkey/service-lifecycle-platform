package com.vyulabs.update.config

import spray.json.DefaultJsonProtocol

case class MaxCpu(percents: Int, durationSec: Int)

object MaxCpu extends DefaultJsonProtocol {
  implicit val maxCpuJson = jsonFormat2(MaxCpu.apply)
}

case class RestartConditionsConfig(maxMemoryMB: Option[Long], maxCpu: Option[MaxCpu]) {
  def maxMemory = maxMemoryMB.map(_ * 1024 *1024)
}

case class RunServiceConfig(command: String, args: Option[Seq[String]], env: Option[Map[String, String]],
                            logWriter: LogWriterConfig, logUploader: Option[LogUploaderConfig], faultFilesMatch: Option[String],
                            restartOnFault: Option[Boolean], restartConditions: Option[RestartConditionsConfig])

object RunServiceConfig extends DefaultJsonProtocol {
  import LogWriterConfig._
  import LogUploaderConfig._

  implicit val restartConditionsConfigJson = jsonFormat2(RestartConditionsConfig.apply)
  implicit val runServiceConfigJson = jsonFormat8(RunServiceConfig.apply)
}
