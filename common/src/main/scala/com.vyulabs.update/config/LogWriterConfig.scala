package com.vyulabs.update.config

import spray.json._

case class LogWriterConfig(directory: String, filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int, dateFormat: Option[String])

object LogWriterConfigJson extends DefaultJsonProtocol {
  implicit val logWriterConfigJson = jsonFormat5(LogWriterConfig)
}
