package com.vyulabs.update.common.config

import spray.json.DefaultJsonProtocol

case class LogUploaderConfig(writer: LogWriterInit)

object LogUploaderConfig extends DefaultJsonProtocol {
  implicit val logWriterInitJson = jsonFormat3(LogWriterInit.apply)
  implicit val logUploaderConfigJson = jsonFormat1(LogUploaderConfig.apply)
}
