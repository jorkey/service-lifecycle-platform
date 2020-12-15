package com.vyulabs.update.distribution.config

import spray.json.DefaultJsonProtocol

case class SslConfig(keyStoreFile: String, keyStorePassword: String)

object SslConfig extends DefaultJsonProtocol {
  implicit val sslConfigJson = jsonFormat2(SslConfig.apply)
}
