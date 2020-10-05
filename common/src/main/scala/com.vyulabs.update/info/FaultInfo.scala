package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import com.vyulabs.update.utils.Utils.DateJson._

import spray.json.DefaultJsonProtocol

case class FaultInfo(date: Date, instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                     serviceName: ServiceName, serviceProfile: ServiceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7(FaultInfo.apply)
}

case class ClientFaultInfo(date: Date, clientName: ClientName,
                           instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                           serviceName: ServiceName, serviceProfile: ServiceProfile,
                           state: ServiceState, logTail: Seq[String])

object ClientFaultInfo extends DefaultJsonProtocol {
  implicit val clientFaultInfoJson = jsonFormat8(ClientFaultInfo.apply)

  def apply(clientName: ClientName, faultInfo: FaultInfo): ClientFaultInfo = {
    ClientFaultInfo(faultInfo.date, clientName,
      faultInfo.instanceId, faultInfo.serviceDirectory,
      faultInfo.serviceName, faultInfo.serviceProfile,
      faultInfo.state, faultInfo.logTail)
  }
}
