package com.vyulabs.update.common.info

import spray.json.DefaultJsonProtocol

case class PrivateFile(service: String, file: String)

object PrivateFile extends DefaultJsonProtocol {
  implicit val versionInfoJson = jsonFormat2(PrivateFile.apply)
}
