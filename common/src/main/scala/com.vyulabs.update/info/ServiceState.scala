package com.vyulabs.update.info

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common._
import com.vyulabs.update.utils.Utils.DateJson.DateJsonFormat
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

case class UpdateError(critical: Boolean, error: String)

object UpdateError extends DefaultJsonProtocol {
  implicit val updateErrorJson = jsonFormat2(UpdateError.apply)
}

case class ServiceState(date: Date, installDate: Option[Date], startDate: Option[Date],
                        version: Option[BuildVersion], updateToVersion: Option[BuildVersion],
                        updateError: Option[UpdateError], failuresCount: Option[Int], lastExitCode: Option[Int])

object ServiceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val stateJson = jsonFormat8(ServiceState.apply)
}

case class DirectoryServiceState(serviceName: ServiceName, directory: ServiceDirectory, state: ServiceState)

object DirectoryServiceState extends DefaultJsonProtocol {
  implicit val serviceStateJson = jsonFormat3(DirectoryServiceState.apply)

  def getServiceInstanceState(serviceName: ServiceName, directory: File)(implicit log: Logger): DirectoryServiceState = {
    val ownState = ServiceState(date = new Date(), installDate = IoUtils.getServiceInstallTime(serviceName, directory),
      startDate = None, version = IoUtils.readServiceVersion(serviceName, directory),
      updateToVersion = None, updateError = None, failuresCount = None, lastExitCode = None)
    DirectoryServiceState(serviceName, directory.getCanonicalPath(), ownState)
  }
}

case class InstanceServiceState(instanceId: InstanceId, serviceName: ServiceName, directory: ServiceDirectory, state: ServiceState)

object InstanceServiceState extends DefaultJsonProtocol {
  implicit val instanceServiceStateJson = jsonFormat4(InstanceServiceState.apply)
}

case class ClientServiceState(clientName: ClientName, instanceId: InstanceId, serviceName: ServiceName, directory: ServiceDirectory, state: ServiceState)

object ClientServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat5(ClientServiceState.apply)

  def apply(clientName: ClientName, instanceId: InstanceId, state: DirectoryServiceState): ClientServiceState = {
    ClientServiceState(clientName, instanceId, state.serviceName, state.directory, state.state)
  }
}