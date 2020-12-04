package com.vyulabs.update.updater.uploaders

import java.io.File

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.distribution.graphql.OldDistributionInterface
import com.vyulabs.update.info.{DirectoryServiceState, ProfiledServiceName}
import com.vyulabs.update.updater.ServiceStateController
import org.slf4j.Logger

class StateUploader(instanceId: InstanceId, servicesNames: Set[ProfiledServiceName],
                    clientDirectory: OldDistributionInterface)(implicit log: Logger) extends Thread { self =>
  private val services = servicesNames.foldLeft(Map.empty[ProfiledServiceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
  }

  /* TODO graphql
  for (serviceStates <- clientDirectory.downloadServicesState(instanceId)) {
    serviceStates.foreach { case state =>
        services.foreach { case (name, controller) =>
          if (name.name == state.serviceName && controller.serviceDirectory.getCanonicalPath == state.directory) {
            controller.initFromState(state.state)
          }
        }
      }
    }
  */

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
    val scriptsState = DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File("."))
    val serviceStates = services.foldLeft(Seq(scriptsState))((state, service) =>
      state :+ DirectoryServiceState(service._1.name, service._2.serviceDirectory.getCanonicalPath, service._2.getState()))
    clientDirectory.uploadServicesStates(serviceStates)
  }
}
