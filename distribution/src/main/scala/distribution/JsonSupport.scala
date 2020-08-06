package distribution

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

case class UserInfo(role: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val userInfo = jsonFormat1(UserInfo)
}
