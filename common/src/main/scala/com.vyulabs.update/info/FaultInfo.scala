package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import com.vyulabs.update.utils.Utils.DateJson._
import spray.json.DefaultJsonProtocol

case class FaultInfo(date: Date,
                     instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                     serviceName: ServiceName, serviceProfile: ServiceProfile,
                     state: ServiceState, logTail: Seq[String])

object FaultInfo extends DefaultJsonProtocol {
  implicit val faultInfoJson = jsonFormat7(FaultInfo.apply)
}

case class ClientFaultReport(clientName: ClientName, directory: String, files: Seq[String],
                             date: Date, instanceId: InstanceId, serviceDirectory: ServiceDirectory,
                             serviceName: ServiceName, serviceProfile: ServiceProfile,
                             state: ServiceState, logTail: Seq[String])

object ClientFaultReport extends DefaultJsonProtocol {
  implicit val clientFaultInfoJson = jsonFormat10(ClientFaultReport.apply)

  def apply(clientName: ClientName, faultInfo: FaultInfo, directory: String, files: Seq[String]): ClientFaultReport = {
    ClientFaultReport(clientName, directory, files,
      faultInfo.date, faultInfo.instanceId, faultInfo.serviceDirectory,
      faultInfo.serviceName, faultInfo.serviceProfile,
      faultInfo.state, faultInfo.logTail)
  }
}
