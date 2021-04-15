package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceDirectory, ServiceId, ServiceProfile}
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

case class FaultInfo(date: Date, instance: InstanceId,
                     service: ServiceId, serviceDirectory: ServiceDirectory, serviceProfile: ServiceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7((date: Date, instance: InstanceId, service: ServiceId, serviceDirectory: ServiceDirectory, serviceProfile: ServiceProfile, state: ServiceState, logTail: Seq[String]) =>
    FaultInfo.apply(date, instance, service, serviceDirectory, serviceProfile, state, logTail))
}