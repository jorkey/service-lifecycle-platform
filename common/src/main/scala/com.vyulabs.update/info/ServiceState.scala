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

case class ServiceState(date: Date = new Date(), installDate: Option[Date] = None, startDate: Option[Date] = None,
                        version: Option[BuildVersion] = None, updateToVersion: Option[BuildVersion] = None,
                        updateError: Option[UpdateError] = None, failuresCount: Option[Int] = None, lastExitCode: Option[Int] = None)

object ServiceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val stateJson = jsonFormat8(ServiceState.apply)
}

case class DirectoryServiceState(serviceName: ServiceName, directory: ServiceDirectory, state: ServiceState)

object DirectoryServiceState extends DefaultJsonProtocol {
  implicit val serviceStateJson = jsonFormat3(DirectoryServiceState.apply)

  def getOwnInstanceState(serviceName: ServiceName, startDate: Date)(implicit log: Logger): DirectoryServiceState = {
    val ownState = ServiceState(version = Utils.getManifestBuildVersion(serviceName),
      installDate = IoUtils.getServiceInstallTime(serviceName, new File(".")),
      startDate = Some(startDate))
    DirectoryServiceState(serviceName, new File(".").getCanonicalPath(), ownState)
  }

  def getServiceInstanceState(serviceName: ServiceName, directory: File)(implicit log: Logger): DirectoryServiceState = {
    val ownState = ServiceState(version = IoUtils.readServiceVersion(serviceName, directory),
      installDate = IoUtils.getServiceInstallTime(serviceName, directory))
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