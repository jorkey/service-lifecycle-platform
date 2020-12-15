package com.vyulabs.update.common.config

import spray.json._

case class LogWriterConfig(directory: String, filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int, dateFormat: Option[String])

object LogWriterConfig extends DefaultJsonProtocol {
  implicit val logWriterConfigJson = jsonFormat5(LogWriterConfig.apply)
}
