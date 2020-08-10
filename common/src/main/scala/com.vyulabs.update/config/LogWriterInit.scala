package com.vyulabs.update.config

import spray.json._

case class LogWriterInit(filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int)

object LogWriterInitJson extends DefaultJsonProtocol {
  implicit val logWriterInitJson = jsonFormat3(LogWriterInit.apply)
}
