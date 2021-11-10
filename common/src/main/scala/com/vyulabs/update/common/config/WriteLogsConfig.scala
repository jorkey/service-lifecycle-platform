package com.vyulabs.update.common.config

import spray.json._

case class WriteLogsConfig(directory: String, filePrefix: String, maxFileSizeMB: Int, maxFilesCount: Int)

object WriteLogsConfig extends DefaultJsonProtocol {
  implicit val writeLogsConfigJson = jsonFormat4(WriteLogsConfig.apply)
}
