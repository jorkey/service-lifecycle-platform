package com.vyulabs.update.updater.uploaders

import java.io.File
import java.util.Date

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{InstanceId}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.info.{ProfiledServiceName, ServicesState}
import com.vyulabs.update.updater.ServiceStateController
import org.slf4j.Logger

class StateUploader(instanceId: InstanceId, servicesNames: Set[ProfiledServiceName],
                    clientDirectory: ClientDistributionDirectoryClient)(implicit log: Logger) extends Thread { self =>
  private val services = servicesNames.foldLeft(Map.empty[ProfiledServiceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
  }

  private var startDate = new Date()

  for (servicesState <- clientDirectory.downloadServicesState(instanceId)) {
    servicesState.directories.foreach { case (directory, serviceStates) =>
      serviceStates.foreach { case (serviceName, serviceState) =>
        if (directory == directory) {
          if (serviceName == Common.UpdaterServiceName) {
            for (date <- serviceState.startDate) {
              startDate = date
            }
          } else {
            services.foreach { case (name, controller) =>
              if (name.name == serviceName && controller.serviceDirectory.getCanonicalPath == directory) {
                controller.initFromState(serviceState)
              }
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
    val scriptsState = ServicesState.getServiceInstanceState(Common.ScriptsServiceName, new File("."))
    val servicesState = services.foldLeft(ServicesState.empty)((state, service) =>
      state.merge(ServicesState(service._1.name, service._2.getState(), service._2.serviceDirectory.getCanonicalPath)))
    clientDirectory.uploadServicesStates(instanceId, scriptsState.merge(servicesState))
  }
}
