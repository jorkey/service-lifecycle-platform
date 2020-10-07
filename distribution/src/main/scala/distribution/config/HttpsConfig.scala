package distribution.config

import spray.json.DefaultJsonProtocol

case class HttpsConfig(port: Int, keyStoreFile: String, keyStorePassword: String)

object HttpsConfig extends DefaultJsonProtocol {
  implicit val httpsConfigJson = jsonFormat3(HttpsConfig.apply)
}
