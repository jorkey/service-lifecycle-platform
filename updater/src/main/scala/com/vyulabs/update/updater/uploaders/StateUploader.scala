package com.vyulabs.update.updater.uploaders

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.state.{ProfiledServiceName, ServiceInstallation, ServiceState, ServicesState}
import com.vyulabs.update.updater.ServiceStateController
import com.vyulabs.update.utils.{IOUtils, Utils}
import org.slf4j.Logger

class StateUploader(instanceId: InstanceId, servicesNames: Set[ProfiledServiceName],
                    clientDirectory: ClientDistributionDirectoryClient)(implicit log: Logger) extends Thread { self =>
  private val services = servicesNames.foldLeft(Map.empty[ProfiledServiceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
  }

  private var startDate = new Date()

  for (servicesState <- clientDirectory.downloadServicesState(instanceId)) {
    val directory = new java.io.File(".").getCanonicalPath()
    servicesState.state.foreach { case (service, serviceState) =>
      if (service.directory == directory) {
        if (service.name.service == Common.UpdaterServiceName) {
          for (date <- serviceState.startDate) {
            startDate = date
          }
        } else {
          for (state <- services.get(service.name)) {
            state.initFromState(serviceState)
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
    val ownState = ServiceState.getOwnInstanceState(Common.UpdaterServiceName)
    val scriptsState = ServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File("."))
    val servicesState = services.foldLeft(Map.empty[ServiceInstallation, ServiceState])((states, service) => { states +
      (ServiceInstallation(service._1, new java.io.File(".").getCanonicalPath()) -> service._2.getState()) })
    val state = ServicesState(ownState ++ scriptsState ++ servicesState)
    clientDirectory.uploadServicesStates(instanceId, state)
  }
}
