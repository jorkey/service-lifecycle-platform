package com.vyulabs.update.logs

import com.vyulabs.update.config.LogWriterInit
import spray.json.DefaultJsonProtocol

import scala.collection._

case class ServiceLogs(writerInit: Option[LogWriterInit], records: Seq[String])

object ServiceLogsJson extends DefaultJsonProtocol {
  import com.vyulabs.update.config.LogWriterInitJson._

  implicit val serviceLogsJson = jsonFormat2(ServiceLogs.apply)
}
