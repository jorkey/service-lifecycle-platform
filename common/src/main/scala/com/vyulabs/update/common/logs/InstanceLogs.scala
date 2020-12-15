package com.vyulabs.update.common.logs

import com.vyulabs.update.common.config.LogWriterInit
import spray.json.DefaultJsonProtocol

import scala.collection._

case class ServiceLogs(writerInit: Option[LogWriterInit], records: Seq[String])

object ServiceLogs extends DefaultJsonProtocol {
  import com.vyulabs.update.common.config.LogWriterInit._

  implicit val serviceLogsJson = jsonFormat2(ServiceLogs.apply)
}
