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

case class InstanceState(time: Date, installTime: Option[Date], startTime: Option[Date],
                         version: Option[ClientDistributionVersion], updateToVersion: Option[ClientDistributionVersion],
                         updateError: Option[UpdateError], failuresCount: Option[Int], lastExitCode: Option[Int])

object InstanceState extends DefaultJsonProtocol {
  implicit val stateJson = jsonFormat8(InstanceState.apply)
}

case class DirectoryServiceState(service: ServiceId, directory: ServiceDirectory, state: InstanceState)

object DirectoryServiceState extends DefaultJsonProtocol {
  implicit val serviceStateJson = jsonFormat3(DirectoryServiceState.apply)

  def getServiceInstanceState(service: ServiceId, directory: File)(implicit log: Logger): DirectoryServiceState = {
    val ownState = InstanceState(time = new Date(), installTime = IoUtils.getServiceInstallTime(service, directory),
      startTime = None, version = IoUtils.readServiceVersion(service, directory),
      updateToVersion = None, updateError = None, failuresCount = None, lastExitCode = None)
    DirectoryServiceState(service, directory.getCanonicalPath(), ownState)
  }
}

case class AddressedInstanceState(instance: InstanceId, service: ServiceId, directory: ServiceDirectory, state: InstanceState)

object AddressedInstanceState extends DefaultJsonProtocol {
  implicit val instanceServiceStateJson = jsonFormat4(AddressedInstanceState.apply)
}