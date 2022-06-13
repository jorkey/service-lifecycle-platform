package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{InstanceId, ServiceDirectory, ServiceId, ServiceRole}
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats._

case class FaultInfo(time: Date, instance: InstanceId,
                     service: ServiceId, serviceRole: Option[ServiceRole], serviceDirectory: ServiceDirectory,
                     state: InstanceState, logTail: Seq[LogLine])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7((time: Date, instance: InstanceId, service: ServiceId, serviceRole: Option[ServiceRole],
                                            serviceDirectory: ServiceDirectory, state: InstanceState, logTail: Seq[LogLine]) =>
    FaultInfo.apply(time, instance, service, serviceRole, serviceDirectory, state, logTail))
}