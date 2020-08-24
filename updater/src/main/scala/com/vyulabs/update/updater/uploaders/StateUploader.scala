package com.vyulabs.update.updater.uploaders

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.state.{ProfiledServiceName, ServiceState, ServicesState}
import com.vyulabs.update.updater.ServiceStateController
import org.slf4j.Logger

class StateUploader(instanceId: InstanceId, servicesNames: Set[ProfiledServiceName],
                    clientDirectory: ClientDistributionDirectoryClient)(implicit log: Logger) extends Thread { self =>
  private val services = servicesNames.foldLeft(Map.empty[ProfiledServiceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
  }

  private var startDate = new Date()

  for (servicesState <- clientDirectory.downloadServicesState(instanceId)) {
    val directory = new java.io.File(".").getCanonicalPath()
    servicesState.directories.foreach { case (directory, serviceStates) =>
      serviceStates.foreach { case (serviceName, serviceState) =>
        if (directory == directory) {
          if (serviceName.service == Common.UpdaterServiceName) {
            for (date <- serviceState.startDate) {
              startDate = date
            }
          } else {
            for (state <- services.get(serviceName)) {
              state.initFromState(serviceState)
            }
          }
        }
      }
    }
  }

  def getServiceStateController(profiledServiceName: ProfiledServiceName): Option[ServiceStateController] = {
    services.get(profiledServiceName)
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
    val ownState = ServicesState.getOwnInstanceState(Common.UpdaterServiceName)
    val scriptsState = ServicesState.getServiceInstanceState(new File("."), Common.ScriptsServiceName)
    val states = services.foldLeft(Map.empty[ProfiledServiceName, ServiceState])((states, service) => {
      states + (service._1 -> service._2.getState()) })
    val servicesState = ServicesState(Map.empty + (new java.io.File(".").getCanonicalPath() -> states))
    clientDirectory.uploadServicesStates(instanceId, ownState.merge(scriptsState).merge(servicesState))
  }
}
