package com.vyulabs.update.info

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common._
import com.vyulabs.update.utils.{IoUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

import scala.collection.mutable

case class ProfiledServiceName(name: ServiceName, profile: ServiceProfile) {
  override def toString: String = {
    if (profile != CommonProfile) {
      name + "-" + profile
    } else {
      name
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

  implicit val serviceStateJson = jsonFormat8(ServiceState.apply)
}

case class ServicesState(directories: Map[ServiceDirectory, Map[ServiceName, ServiceState]]) {
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

  def empty = ServicesState(Map.empty)

  def apply(name: ServiceName, state: ServiceState, directory: ServiceDirectory): ServicesState = {
    val states = Map.empty[ServiceName, ServiceState] + (name -> state)
    val directories = Map.empty[ServiceDirectory, Map[ServiceName, ServiceState]] + (directory -> states)
    ServicesState(directories)
  }

  def getOwnInstanceState(serviceName: ServiceName, startDate: Date)(implicit log: Logger): ServicesState = {
    val ownState = ServiceState(version = Utils.getManifestBuildVersion(serviceName),
      installDate = IoUtils.getServiceInstallTime(serviceName, new File(".")),
      startDate = Some(startDate))
    val directoryState = Map.empty + (serviceName -> ownState)
    ServicesState(Map.empty + (new File(".").getCanonicalPath() -> directoryState))
  }

  def getServiceInstanceState(serviceName: ServiceName, directory: File)(implicit log: Logger): ServicesState = {
    val ownState = ServiceState(version = IoUtils.readServiceVersion(serviceName, directory),
      installDate = IoUtils.getServiceInstallTime(serviceName, directory))
    val directoryState = Map.empty + (serviceName -> ownState)
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

  def addState(instanceId: InstanceId, state: ServicesState): InstancesState = {
    var mergedInstances = instances
    val mergedState = mergedInstances.get(instanceId) match {
      case Some(instance) =>
        instance.merge(state)
      case None =>
        state
    }
    mergedInstances += (instanceId -> mergedState)
    InstancesState(mergedInstances)
  }
}

object InstancesState extends DefaultJsonProtocol {
  implicit val instancesStateJson = jsonFormat1(InstancesState.apply)

  def empty = InstancesState(Map.empty)
}

case class InstanceVersions(versions: Map[ServiceName, Map[BuildVersion, Map[ServiceDirectory, Set[InstanceId]]]]) {
  def addVersions(instanceId: InstanceId, state: ServicesState): InstanceVersions = {
    var newVersions = versions
    state.directories.foreach { case (directory, state) =>
      state.foreach { case (name, state) =>
        val version = state.version.getOrElse(BuildVersion.empty)
        var versionMap = newVersions.getOrElse(name, Map.empty[BuildVersion, Map[ServiceDirectory, Set[InstanceId]]])
        var dirMap = versionMap.getOrElse(version, Map.empty[ServiceDirectory, Set[InstanceId]])
        dirMap += (directory -> (dirMap.getOrElse(directory, Set.empty) + instanceId))
        versionMap += (version -> dirMap)
        newVersions += (name -> versionMap)
      }
    }
    InstanceVersions(newVersions)
  }
}

object InstanceVersions extends DefaultJsonProtocol {
  import com.vyulabs.update.version.BuildVersion._

  implicit val clientInstancesStateJson = jsonFormat1(InstanceVersions.apply)

  def empty = InstanceVersions(Map.empty)
}