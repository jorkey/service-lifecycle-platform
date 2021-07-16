package com.vyulabs.update.distribution.graphql

import spray.json.{DefaultJsonProtocol, JsObject, JsValue}

trait GraphqlMessage {
  val `type`: String
}

trait InputGraphqlMessage extends GraphqlMessage
trait OutputGraphqlMessage extends GraphqlMessage

case class ConnectionInitPayload(Authorization: String)

object ConnectionInitPayload extends DefaultJsonProtocol {
  implicit val queryJson = jsonFormat1(ConnectionInitPayload.apply)
}

case class ConnectionInit(`type`: String = ConnectionInit.`type`, payload: ConnectionInitPayload) extends InputGraphqlMessage

object ConnectionInit extends DefaultJsonProtocol {
  val `type` = "connection_init"
  implicit val queryJson = jsonFormat2(ConnectionInit.apply)
}

case class ConnectionAck(`type`: String = ConnectionAck.`type`) extends OutputGraphqlMessage

object ConnectionAck extends DefaultJsonProtocol {
  val `type` = "connection_ack"
  implicit val queryJson = jsonFormat1(ConnectionAck.apply)
}

case class SubscribePayload(query: String, operationName: Option[String], variables: Option[JsObject], extensions: Option[JsObject])

object SubscribePayload extends DefaultJsonProtocol {
  implicit val queryJson = jsonFormat4(SubscribePayload.apply)
}

case class Subscribe(`type`: String = Subscribe.`type`, id: String, payload: SubscribePayload) extends InputGraphqlMessage

object Subscribe extends DefaultJsonProtocol {
  val `type` = "subscribe"
  implicit val queryJson = jsonFormat3(Subscribe.apply)
}

case class Next(`type`: String = Next.`type`, id: String, payload: JsObject) extends OutputGraphqlMessage

object Next extends DefaultJsonProtocol {
  val `type` = "next"
  implicit val queryJson = jsonFormat3(Next.apply)
}

case class ErrorPayload(message: String)

object ErrorPayload extends DefaultJsonProtocol {
  implicit val queryJson = jsonFormat1(ErrorPayload.apply)
}

case class Error(`type`: String = Error.`type`, id: String, payload: ErrorPayload) extends OutputGraphqlMessage

object Error extends DefaultJsonProtocol {
  val `type` = "error"
  implicit val queryJson = jsonFormat3(Error.apply)
}

case class Complete(`type`: String = Complete.`type`, id: String) extends InputGraphqlMessage with OutputGraphqlMessage

object Complete extends DefaultJsonProtocol {
  val `type` = "complete"
  implicit val queryJson = jsonFormat2(Complete.apply)
}

case class Ping(`type`: String = Ping.`type`) extends InputGraphqlMessage with OutputGraphqlMessage

object Ping extends DefaultJsonProtocol {
  val `type` = "ping"
  implicit val queryJson = jsonFormat1(Ping.apply)
}

case class Pong(`type`: String = Pong.`type`) extends InputGraphqlMessage with OutputGraphqlMessage

object Pong extends DefaultJsonProtocol {
  val `type` = "pong"
  implicit val queryJson = jsonFormat1(Pong.apply)
}
