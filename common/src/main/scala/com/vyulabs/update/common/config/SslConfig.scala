package com.vyulabs.update.common.config

import spray.json.DefaultJsonProtocol

case class SslConfig(keyStoreFile: String, keyStorePassword: Option[String])

object SslConfig extends DefaultJsonProtocol {
  implicit val sslConfigJson = jsonFormat2(SslConfig.apply)
}
