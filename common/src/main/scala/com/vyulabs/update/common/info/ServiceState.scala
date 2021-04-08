package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.ClientDistributionVersion
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

import java.io.File
import java.util.Date

case class UpdateError(critical: Boolean, error: String)

object UpdateError extends DefaultJsonProtocol {
  implicit val updateErrorJson = jsonFormat2(UpdateError.apply)
}

case class ServiceState(date: Date, installDate: Option[Date], startDate: Option[Date],
                        version: Option[ClientDistributionVersion], updateToVersion: Option[ClientDistributionVersion],
                        updateError: Option[UpdateError], failuresCount: Option[Int], lastExitCode: Option[Int])

object ServiceState extends DefaultJsonProtocol {
  implicit val stateJson = jsonFormat8(ServiceState.apply)
}

case class DirectoryServiceState(service: ServiceName, directory: ServiceDirectory, state: ServiceState)

object DirectoryServiceState extends DefaultJsonProtocol {
  implicit val serviceStateJson = jsonFormat3(DirectoryServiceState.apply)

  def getServiceInstanceState(service: ServiceName, directory: File)(implicit log: Logger): DirectoryServiceState = {
    val ownState = ServiceState(date = new Date(), installDate = IoUtils.getServiceInstallTime(service, directory),
      startDate = None, version = IoUtils.readServiceVersion(service, directory),
      updateToVersion = None, updateError = None, failuresCount = None, lastExitCode = None)
    DirectoryServiceState(service, directory.getCanonicalPath(), ownState)
  }
}

case class InstanceServiceState(instance: InstanceId, service: ServiceName, directory: ServiceDirectory, state: ServiceState)

object InstanceServiceState extends DefaultJsonProtocol {
  implicit val instanceServiceStateJson = jsonFormat4(InstanceServiceState.apply)
}