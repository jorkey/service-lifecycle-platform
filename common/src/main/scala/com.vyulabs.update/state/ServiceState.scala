package com.vyulabs.update.state

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common.{CommonProfile, InstanceId, ServiceDirectory, ServiceName, ServiceProfile}
import com.vyulabs.update.utils.{IOUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

case class ProfiledServiceName(service: ServiceName, profile: ServiceProfile) {
  override def toString: String = {
    if (profile != CommonProfile) {
      service + "-" + profile
    } else {
      service
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

case class ServiceState(date: Date = new Date(), startDate: Option[Date] = None, version: Option[BuildVersion] = None, updateToVersion: Option[BuildVersion] = None,
                        failuresCount: Option[Int] = None, lastErrors: Option[Seq[String]] = None, lastExitCode: Option[Int] = None)

object ServiceState extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val serviceStateJson = jsonFormat7(ServiceState.apply)
}

case class ServicesState(directories: Map[ServiceDirectory, Map[ProfiledServiceName, ServiceState]]) {
  def merge(servicesState: ServicesState): ServicesState = {
    var mergedState = directories
    servicesState.directories.foreach { case (directory, directoryState) =>
      val mergedDirectoryState = mergedState.get(directory) match {
        case Some(mergedDirectoryState) =>
          mergedDirectoryState ++ directoryState
        case None =>
          directoryState
      }
      mergedState += (directory -> mergedDirectoryState)
    }
    ServicesState(mergedState)
  }
}

object ServicesState extends DefaultJsonProtocol {
  import ServiceState._

  implicit val servicesStateJson = jsonFormat1(ServicesState.apply)

  def getOwnInstanceState(serviceName: ServiceName)(implicit log: Logger): ServicesState = {
    val ownState = ServiceState(version = Utils.getManifestBuildVersion(serviceName))
    val directoryState = Map.empty + (ProfiledServiceName(serviceName) -> ownState)
    ServicesState(Map.empty + (new File(".").getCanonicalPath() -> directoryState))
  }

  def getServiceInstanceState(directory: File, serviceName: ServiceName)(implicit log: Logger): ServicesState = {
    val ownState = ServiceState(version = Utils.getManifestBuildVersion(serviceName))
    val directoryState = Map.empty + (ProfiledServiceName(serviceName) -> ownState)
    ServicesState(Map.empty + (directory.getCanonicalPath() -> directoryState))
  }
}

case class InstancesState(instances: Map[InstanceId, ServicesState]) {
  def merge(instancesState: InstancesState): InstancesState = {
    var mergedState = instances
    instancesState.instances.foreach { case (instanceId, instanceState) =>
      mergedState.get(instanceId) match {
        case Some(mergedInstanceState) =>
          mergedState += (instanceId -> mergedInstanceState.merge(instanceState))
        case None =>
          mergedState += (instanceId -> instanceState)
      }
    }
    InstancesState(mergedState)
  }
}

object InstancesState extends DefaultJsonProtocol {
  implicit val instancesStateJson = jsonFormat1(InstancesState.apply)

  def getOwnInstanceState(instanceId: InstanceId, serviceName: ServiceName)(implicit log: Logger): InstancesState = {
    InstancesState(Map.empty + (instanceId -> ServicesState.getOwnInstanceState(serviceName)))
  }

  def getServiceInstanceState(instanceId: InstanceId, directory: File, serviceName: ServiceName)(implicit log: Logger): InstancesState = {
    InstancesState(Map.empty + (instanceId -> ServicesState.getServiceInstanceState(directory, serviceName)))
  }
}

case class InstanceVersionsState(versions: Map[ServiceName, Map[BuildVersion, Set[InstanceId]]])

object InstanceVersionsState extends DefaultJsonProtocol {
  import com.vyulabs.update.version.BuildVersion._

  implicit val clientInstancesStateJson = jsonFormat1(InstanceVersionsState.apply)
}