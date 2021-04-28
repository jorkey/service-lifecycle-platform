package com.vyulabs.update.common.config

import spray.json.DefaultJsonProtocol

case class MaxCpu(percents: Int, duration: Int)

object MaxCpu extends DefaultJsonProtocol {
  implicit val maxCpuJson = jsonFormat2(MaxCpu.apply)
}

case class RestartConditions(maxMemoryMB: Option[Long], maxCpu: Option[MaxCpu], makeCore: Boolean, checkTimeoutMs: Long) {
  def maxMemory = maxMemoryMB.map(_ * 1024 *1024)
}

case class RunServiceConfig(command: String, args: Option[Seq[String]], env: Option[Map[String, String]],
                            logWriter: Option[LogWriterConfig], uploadLogs: Option[Boolean], faultFilesMatch: Option[String],
                            restartOnFault: Option[Boolean], restartConditions: Option[RestartConditions])

object RunServiceConfig extends DefaultJsonProtocol {
  import LogWriterConfig._

  implicit val restartConditionsConfigJson = jsonFormat4(RestartConditions.apply)
  implicit val runServiceConfigJson = jsonFormat8(RunServiceConfig.apply)
}
