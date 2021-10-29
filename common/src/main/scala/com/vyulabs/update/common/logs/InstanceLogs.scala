package com.vyulabs.update.common.logs

import spray.json.DefaultJsonProtocol
import scala.collection._

case class ServiceLogs(records: Seq[String])

object ServiceLogs extends DefaultJsonProtocol {
  implicit val serviceLogsJson = jsonFormat1(ServiceLogs.apply)
}
