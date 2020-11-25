package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import com.vyulabs.update.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

case class FaultInfo(date: Date,
                     instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                     serviceName: ServiceName, serviceProfile: ServiceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7(FaultInfo.apply)
}