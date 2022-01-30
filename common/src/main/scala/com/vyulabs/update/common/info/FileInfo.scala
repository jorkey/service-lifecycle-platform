package com.vyulabs.update.common.info

import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats.DateJsonFormat

import java.util.Date

case class FileInfo(path: String, time: Date, length: Long)

object FileInfo extends DefaultJsonProtocol {
  implicit val fileInfoJson = jsonFormat3(FileInfo.apply)
}
