package com.vyulabs.update.logs

import com.vyulabs.update.config.LogWriterInit
import spray.json.DefaultJsonProtocol

import scala.collection._

case class ServiceLogs(writerInit: Option[LogWriterInit], records: Seq[String])

object ServiceLogs extends DefaultJsonProtocol {
  import com.vyulabs.update.config.LogWriterInit._

  implicit val serviceLogsJson = jsonFormat2(ServiceLogs.apply)
}
