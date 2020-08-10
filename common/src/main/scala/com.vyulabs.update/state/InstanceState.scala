package com.vyulabs.update.state

import java.util.Date

import com.vyulabs.update.common.Common.{InstanceId, UpdaterInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class StateEvent(date: Date, message: String)

case class ServiceState(serviceInstanceName: ServiceInstanceName, startDate: Option[Date],
                        version: Option[BuildVersion], updateToVersion: Option[BuildVersion],
                        failuresCount: Int, lastErrors: Seq[String], lastExitCode: Option[Int])

case class InstanceState(date: Date, startDate: Date, instanceId: InstanceId, directory: String,
                         servicesStates: Seq[ServiceState])

case class InstancesState(states: Map[UpdaterInstanceId, InstanceState])

object ServiceStateJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersionJson._
  import com.vyulabs.update.common.ServiceInstanceJson._

  implicit val serviceStateJson = jsonFormat7(ServiceState)
}

object InstanceStateJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import ServiceStateJson._

  implicit val instanceStateJson = jsonFormat5(InstanceState.apply)
}

object InstancesStateJson extends DefaultJsonProtocol {
  import com.vyulabs.update.common.Common.UpdaterInstanceIdJson._
  import InstanceStateJson._

  implicit val instancesStateJson = jsonFormat1(InstancesState.apply)
}
