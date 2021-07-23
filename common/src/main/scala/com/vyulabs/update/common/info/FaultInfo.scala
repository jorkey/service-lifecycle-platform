package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceDirectory, ServiceId, ServiceInstanceProfile}
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

case class FaultInfo(time: Date, instance: InstanceId,
                     service: ServiceId, serviceDirectory: ServiceDirectory, serviceProfile: ServiceInstanceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7((time: Date, instance: InstanceId, service: ServiceId, serviceDirectory: ServiceDirectory, serviceProfile: ServiceInstanceProfile, state: ServiceState, logTail: Seq[String]) =>
    FaultInfo.apply(time, instance, service, serviceDirectory, serviceProfile, state, logTail))
}