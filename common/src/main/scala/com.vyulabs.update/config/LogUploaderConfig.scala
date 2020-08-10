package com.vyulabs.update.config

import spray.json.DefaultJsonProtocol

case class LogUploaderConfig(writer: LogWriterInit)

object LogUploaderConfigJson extends DefaultJsonProtocol {
  implicit val logWriterInitJson = jsonFormat3(LogWriterInit.apply)
  implicit val logUploaderConfigJson = jsonFormat1(LogUploaderConfig)
}
