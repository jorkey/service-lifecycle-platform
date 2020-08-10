package com.vyulabs.update.config

import spray.json.DefaultJsonProtocol

case class CommandConfig(command: String, args: Seq[String], env: Map[String, String], directory: Option[String],
                         exitCode: Option[Int], outputMatch: Option[String])

object CommandConfigJson extends DefaultJsonProtocol {
  implicit val commandConfigJson = jsonFormat6(CommandConfig)
}
