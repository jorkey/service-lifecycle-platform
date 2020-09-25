package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory}
import spray.json.DefaultJsonProtocol

import ProfiledServiceName._
import ServiceState._

import com.vyulabs.update.utils.Utils.DateJson._

case class FaultInfo(date: Date, instanceId: InstanceId, serviceDirectory: ServiceDirectory, profiledServiceName: ProfiledServiceName,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat6(FaultInfo.apply)
}