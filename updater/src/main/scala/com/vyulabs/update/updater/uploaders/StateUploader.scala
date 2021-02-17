package com.vyulabs.update.updater.uploaders

import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.distribution.client.graphql.ServiceGraphqlCoder.{serviceMutations}
import com.vyulabs.update.common.distribution.client.{SyncDistributionClient, SyncSource}
import com.vyulabs.update.common.info.{DirectoryServiceState, InstanceServiceState, ProfiledServiceName}
import com.vyulabs.update.updater.ServiceStateController
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._

import java.io.File

class StateUploader(instanceId: InstanceId, servicesNames: Set[ProfiledServiceName],
                    distributionClient: SyncDistributionClient[SyncSource])(implicit log: Logger) extends Thread { self =>
  private val services = servicesNames.foldLeft(Map.empty[ProfiledServiceName, ServiceStateController]){ (services, name) =>
    services + (name -> new ServiceStateController(name, () => update()))
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
    val scriptsState = DirectoryServiceState.getServiceInstanceState(Common.ScriptsServiceName, new File("."))
    val serviceStates = services.foldLeft(Seq(scriptsState))((state, service) =>
      state :+ DirectoryServiceState(service._1.name, service._2.serviceDirectory.getCanonicalPath, service._2.getState()))
    val instanceServiceStates = serviceStates.map(state => InstanceServiceState(instanceId, state.serviceName, state.directory, state.state))
    distributionClient.graphqlRequest(serviceMutations.setServiceStates(instanceServiceStates)).getOrElse(false)
  }
}
