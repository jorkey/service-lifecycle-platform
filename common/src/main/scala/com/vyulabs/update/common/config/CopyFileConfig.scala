package com.vyulabs.update.common.config

import spray.json.DefaultJsonProtocol

case class CopyFileConfig(sourceFile: String, destinationFile: String, except: Option[Set[String]], settings: Option[Map[String, String]])

object CopyFileConfig extends DefaultJsonProtocol {
  implicit val copyFileConfigJson = jsonFormat4(CopyFileConfig.apply)
}

