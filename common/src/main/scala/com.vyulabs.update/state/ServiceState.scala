package com.vyulabs.update.state

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{CommonProfile, InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

case class ProfiledServiceName(serviceName: ServiceName, serviceProfile: ServiceProfile) {
  override def toString: String = {
    if (serviceProfile != CommonProfile) {
      serviceName + "-" + serviceProfile
    } else {
      serviceName
    }
  }
}

object ProfiledServiceName {
  implicit object ServiceInstanceNameJsonFormat extends RootJsonFormat[ProfiledServiceName] {
    def write(value: ProfiledServiceName) = JsString(value.toString)
    def read(value: JsValue) = ProfiledServiceName.parse(value.asInstanceOf[JsString].value)
  }

  def apply(serviceName: ServiceName): ProfiledServiceName = {
    ProfiledServiceName(serviceName, CommonProfile)
  }

  def apply(serviceName: ServiceName, serviceProfile: ServiceProfile): ProfiledServiceName = {
    new ProfiledServiceName(serviceName, serviceProfile)
  }

  def parse(name: String): ProfiledServiceName = {
    val fields = name.split("-")
    if (fields.size == 1) {
      ProfiledServiceName(fields(0), CommonProfile)
    } else if (fields.size == 2) {
      ProfiledServiceName(fields(0), fields(1))
    } else {
      sys.error(s"Invalid service instance name ${name}")
    }
  }
}

case class StateEvent(date: Date, message: String)

case class ServiceInstallation(name: ProfiledServiceName, directory: ServiceDirectory)

object ServiceInstallation extends DefaultJsonProtocol {
  implicit val serviceInstallation = jsonFormat2(ServiceInstallation.apply)
}

case class ServiceState(date: Date = new Date(), startDate: Option[Date] = None, version: Option[BuildVersion] = None, updateToVersion: Option[BuildVersion] = None,
                        failuresCount: Option[Int] = None, lastErrors: Option[Seq[String]] = None, lastExitCode: Option[Int] = None)

object ServiceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val serviceStateJson = jsonFormat7(ServiceState.apply)

  def getOwnInstanceState(serviceName: ServiceName)(implicit log: Logger): Map[ServiceInstallation, ServiceState] = {
    val ownInstallation = ServiceInstallation(ProfiledServiceName(serviceName), new java.io.File(".").getCanonicalPath())
    val ownState = ServiceState(version = Utils.getManifestBuildVersion(serviceName))
    Map.empty[ServiceInstallation, ServiceState] + (ownInstallation -> ownState)
  }

  def getServiceInstanceState(serviceName: ServiceName, directory: File)(implicit log: Logger): Map[ServiceInstallation, ServiceState] = {
    val serviceInstallation = ServiceInstallation(ProfiledServiceName(serviceName), directory.getCanonicalPath())
    val serviceState = ServiceState(version = IOUtils.readServiceVersion(serviceName, directory))
    Map.empty[ServiceInstallation, ServiceState] + (serviceInstallation -> serviceState)
  }
}

case class ServicesState(state: Map[ServiceInstallation, ServiceState])

object ServicesState extends DefaultJsonProtocol {
  import ServiceState._

  implicit val servicesStateJson = jsonFormat1(ServicesState.apply)
}

case class InstancesState(state: Map[InstanceId, Map[ServiceInstallation, ServiceState]])

object InstancesState extends DefaultJsonProtocol {
  import ServiceState._

  implicit val instancesStateJson = jsonFormat1(InstancesState.apply)
}

case class InstanceVersionsState(versions: Map[ServiceName, Map[BuildVersion, Set[InstanceId]]])

object InstanceVersionsState extends DefaultJsonProtocol {
  import com.vyulabs.update.version.BuildVersion._

  implicit val clientInstancesStateJson = jsonFormat1(InstanceVersionsState.apply)
}