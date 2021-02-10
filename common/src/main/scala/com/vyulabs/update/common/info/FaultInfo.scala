package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

case class FaultInfo(date: Date,
                     instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                     serviceName: ServiceName, serviceProfile: ServiceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7(FaultInfo.apply)
}