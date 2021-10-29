package com.vyulabs.update.common.config

import spray.json.DefaultJsonProtocol

case class CommandConfig(command: String, args: Option[Seq[String]], env: Option[Map[String, String]], directory: Option[String],
                         exitCode: Option[Int], outputMatch: Option[String])

object CommandConfig extends DefaultJsonProtocol {
  implicit val commandConfigJson = jsonFormat6(CommandConfig.apply)
}
