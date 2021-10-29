package com.vyulabs.update.common.config

import spray.json._

case class LogWriterConfig(directory: String, filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int)

object LogWriterConfig extends DefaultJsonProtocol {
  implicit val logWriterConfigJson = jsonFormat4(LogWriterConfig.apply)
}
