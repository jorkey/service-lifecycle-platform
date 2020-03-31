package com.vyulabs.update.updater

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.common.{Common, ServiceInstanceName}
import com.vyulabs.update.state.{InstanceState, ServiceState}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.updater.UpdaterMain.log
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 10.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceStateController(serviceInstanceName: ServiceInstanceName,
                             updateRepository: () => Unit)(implicit log: Logger) {
  val serviceDirectory = new File(serviceInstanceName.toString)
  val currentServiceDirectory = new File(serviceDirectory, "current")
  val faultsDirectory = new File(serviceDirectory, "faults")
  val newServiceDirectory = new File(serviceDirectory, "new")
  val logHistoryDirectory = new File(serviceDirectory, "log.history")

  @volatile private var startDate = Option.empty[Date]
  @volatile private var version = Option.empty[BuildVersion]
  @volatile private var updateToVersion = Option.empty[BuildVersion]
  @volatile private var lastErrors = Seq.empty[String]
  @volatile private var lastExitCode = Option.empty[Int]
  @volatile private var failuresCount = 0

  private val maxLastErrors = 25

  if (!serviceDirectory.exists() && !serviceDirectory.mkdir()) {
    sys.error(s"Can't create directory ${serviceDirectory}")
  }

  version = if (serviceInstanceName.serviceName == Common.UpdaterServiceName) {
    Utils.getManifestBuildVersion(Common.UpdaterServiceName)
  } else {
    Utils.readServiceVersion(currentServiceDirectory)
  }

  log.info(s"Current version of service ${serviceInstanceName} is ${version}")

  def getVersion() = version

  def initFromState(state: ServiceState): Unit = {
    failuresCount = state.failuresCount
    if (serviceInstanceName.serviceName == Common.UpdaterServiceName) {
      if (updateToVersion.isEmpty) {
        failuresCount += 1
      }
    }
  }

  def setVersion(version: BuildVersion): Unit = synchronized {
    this.version = Some(version)
    if (updateToVersion.isDefined) {
      info(s"Updated to version ${version}")
      updateToVersion = None
    } else {
      info(s"Installed version ${version}")
    }
    updateRepository()
  }

  def serviceStarted(): Unit = synchronized {
    this.startDate = Some(new Date())
    updateRepository()
  }

  def serviceStopped(): Unit = synchronized {
    this.startDate = None
    updateRepository()
  }

  def setUpdateToVersion(serviceVersion: BuildVersion): Unit = synchronized {
    this.updateToVersion = Some(serviceVersion)
    info(s"Begin update to version ${serviceVersion}")
    updateRepository()
  }

  def info(message: String): Unit = {
    log.info(s"Service ${serviceInstanceName}: ${message}")
  }

  def error(message: String): Unit = {
    log.error(s"Service ${serviceInstanceName}: ${message}")
    addLastError(message)
    updateRepository()
  }

  def error(message: String, exception: Throwable): Unit = {
    log.error(s"Service ${serviceInstanceName}: ${message}", exception)
    addLastError(message + ": " + exception.toString)
    updateRepository()
  }

  def failure(exitCode: Int): Unit = {
    log.error(s"Service ${serviceInstanceName} terminated unexpectedly with code ${exitCode}")
    lastExitCode = Some(exitCode)
    failuresCount += 1
    updateRepository()
  }

  def getState(): ServiceState = {
    ServiceState(serviceInstanceName, startDate, version, updateToVersion, failuresCount, lastErrors, lastExitCode)
  }

  private def addLastError(error: String): Unit = {
    lastErrors :+= error
    if (lastErrors.size == maxLastErrors + 1) {
      lastErrors = lastErrors.drop(1)
    }
  }
}

class InstanceStateUploader(instanceId: InstanceId, version: BuildVersion,
                            servicesInstanceNames: Set[ServiceInstanceName],
                            clientDirectory: ClientDistributionDirectoryClient)(implicit log: Logger) extends Thread { self =>
  private val services = servicesInstanceNames.foldLeft(Map.empty[ServiceInstanceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
  }

  private val startDate = clientDirectory.downloadInstanceState(instanceId) match {
    case Some(instanceState) =>
      for (storedState <- instanceState.servicesStates) {
        val serviceName = storedState.serviceInstanceName
        for (state <- services.get(serviceName)) {
          state.initFromState(storedState)
        }
      }
      instanceState.startDate
    case None =>
      new Date()
  }

  def getServiceStateController(serviceInstanceName: ServiceInstanceName): Option[ServiceStateController] = {
    services.get(serviceInstanceName)
  }

  def error(message: String, exception: Throwable): Unit = {
    log.error(message, exception)
    update()
  }

  def update(): Unit = {
    self.synchronized {
      self.notify()
    }
  }

  override def run(): Unit = {
    while (true) {
      try {
        self.synchronized {
          self.wait(10000)
        }
        updateRepository()
      } catch {
        case ex: Exception =>
          log.error("Updating repository error", ex)
      }
    }
  }

  private def updateRepository(): Boolean = synchronized {
    log.info("Update instance state")
    val state = InstanceState(new Date(), startDate, instanceId, new java.io.File(".").getCanonicalPath(),
      services.foldLeft(Seq.empty[ServiceState])((states, service) => { states :+ service._2.getState() }))
    clientDirectory.uploadInstanceState(instanceId, ProcessHandle.current().pid().toString, state)
  }
}
