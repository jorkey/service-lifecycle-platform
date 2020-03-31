package com.vyulabs.update.state

import java.util.Date

import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import com.vyulabs.update.common.Common.{InstanceId, UpdaterInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion

import scala.collection.JavaConverters._

case class StateEvent(date: Date, message: String)

case class ServiceState(serviceInstanceName: ServiceInstanceName, startDate: Option[Date],
                        version: Option[BuildVersion], updateToVersion: Option[BuildVersion],
                        failuresCount: Int, lastErrors: Seq[String], lastExitCode: Option[Int]) {
  def toConfig(): Config = {
    var service = ConfigFactory.empty()
      .withValue("service", ConfigValueFactory.fromAnyRef(serviceInstanceName.toString))
    for (startDate <- startDate) {
      service = service.withValue("startDate", ConfigValueFactory.fromAnyRef(Utils.serializeISO8601Date(startDate)))
    }
    for (serviceVersion <- version) {
      service = service.withValue("version", ConfigValueFactory.fromAnyRef(serviceVersion.toString))
    }
    for (updateToVersion <- updateToVersion) {
      service = service.withValue("updateToVersion", ConfigValueFactory.fromAnyRef(updateToVersion.toString))
    }
    service = service.withValue("failuresCount", ConfigValueFactory.fromAnyRef(failuresCount))
    if (!lastErrors.isEmpty) {
      service = service.withValue("lastErrors", ConfigValueFactory.fromIterable(lastErrors.asJava))
    }
    for (lastExitCode <- lastExitCode) {
      service = service.withValue("lastExitCode", ConfigValueFactory.fromAnyRef(lastExitCode.toString))
    }
    service
  }
}

object ServiceState {
  def apply(config: Config): ServiceState = {
    val serviceInstanceName = ServiceInstanceName.parse(config.getString("service"))
    val startDate = if (config.hasPath("startDate")) Some(Utils.parseISO8601Date(config.getString("startDate"))) else None
    val version = if (config.hasPath("version")) Some(BuildVersion.parse(config.getString("version"))) else None
    val updateToVersion = if (config.hasPath("updateToVersion")) Some(BuildVersion.parse(config.getString("updateToVersion"))) else None
    val failuresCount = config.getInt("failuresCount")
    val lastErrors = if (config.hasPath("lastErrors")) config.getStringList("lastErrors").asScala else Seq.empty
    val lastExitCode = if (config.hasPath("lastExitCode")) Some(config.getInt("lastExitCode")) else None
    ServiceState(serviceInstanceName, startDate, version, updateToVersion, failuresCount, lastErrors, lastExitCode)
  }
}

case class InstanceState(date: Date, startDate: Date, instanceId: InstanceId, directory: String,
                         servicesStates: Seq[ServiceState]) {
  def toConfig(): Config = {
    var instance = ConfigFactory.empty()
      .withValue("date", ConfigValueFactory.fromAnyRef(Utils.serializeISO8601Date(date)))
      .withValue("startDate", ConfigValueFactory.fromAnyRef(Utils.serializeISO8601Date(startDate)))
      .withValue("instanceId", ConfigValueFactory.fromAnyRef(instanceId))
      .withValue("directory", ConfigValueFactory.fromAnyRef(directory))
    instance = instance.withValue("services", ConfigValueFactory.fromIterable(
      servicesStates.map(_.toConfig().root()).asJava))
    ConfigFactory.empty().withValue("instance", instance.root())
  }
}

object InstanceState {
  def apply(config: Config): InstanceState = {
    val instance = config.getConfig("instance")
    val date = Utils.parseISO8601Date(instance.getString("date"))
    val startDate = Utils.parseISO8601Date(instance.getString("startDate"))
    val instanceId = instance.getString("instanceId")
    val directory = if (instance.hasPath("directory")) instance.getString("directory") else "/"
    val services = instance.getConfigList("services").asScala.map(ServiceState(_)).
      foldLeft(Seq.empty[ServiceState])((seq, state) => seq :+ state)
    new InstanceState(date, startDate, instanceId, directory, services)
  }
}

case class InstancesState(states: Map[UpdaterInstanceId, InstanceState]) {
  def toConfig(): Config = {
    val config = ConfigFactory.empty()
    val list = ConfigValueFactory.fromIterable(states.values.map(_.toConfig().root()).asJava)
    config.withValue("instances", list)
  }
}

object InstancesState {
  def apply(config: Config): InstancesState = {
    val states = config.getConfigList("instances").asScala.map(InstanceState(_))
      .foldLeft(Map.empty[UpdaterInstanceId, InstanceState])((map, state) => {
        map + (UpdaterInstanceId(state.instanceId, state.directory) -> state)
      })
    InstancesState(states)
  }
}
