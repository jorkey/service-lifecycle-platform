package com.vyulabs.update.state

import java.util.Date

import com.vyulabs.update.common.Common.{VmInstanceId, ServiceName, UpdaterDirectory}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class StateEvent(date: Date, message: String)

case class ServiceState(serviceInstanceName: ServiceInstanceName, startDate: Option[Date],
                        version: Option[BuildVersion], updateToVersion: Option[BuildVersion],
                        failuresCount: Int, lastErrors: Seq[String], lastExitCode: Option[Int])

object ServiceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersionJson._
  import com.vyulabs.update.common.ServiceInstanceName._

  implicit val serviceStateJson = jsonFormat7(ServiceState.apply)
}

case class UpdaterInstanceState(date: Date, startDate: Date, servicesStates: Seq[ServiceState])

object UpdaterInstanceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import ServiceState._

  implicit val instanceStateJson = jsonFormat3(UpdaterInstanceState.apply)
}

case class VmInstancesState(state: Map[VmInstanceId, Map[UpdaterDirectory, UpdaterInstanceState]])

object VmInstancesState extends DefaultJsonProtocol {
  import UpdaterInstanceState._

  implicit val instancesStateJson = jsonFormat1(VmInstancesState.apply)
}

case class VmInstanceVersionsState(versions: Map[ServiceName, Map[BuildVersion, Set[VmInstanceId]]])

object VmInstanceVersionsState extends DefaultJsonProtocol {
  import com.vyulabs.update.version.BuildVersionJson._

  implicit val clientInstancesStateJson = jsonFormat1(VmInstanceVersionsState.apply)
}