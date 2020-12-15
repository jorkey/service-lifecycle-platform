package com.vyulabs.update.common.config

import spray.json._

case class LogWriterInit(filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int)

object LogWriterInit extends DefaultJsonProtocol {
  implicit val logWriterInitJson = jsonFormat3(LogWriterInit.apply)
}
