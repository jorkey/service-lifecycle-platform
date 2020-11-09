package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ClientName, InstanceId}
import spray.json.DefaultJsonProtocol

case class ClientServiceState(clientName: ClientName, instanceState: InstanceServiceState)

object ClientServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat2(ClientServiceState.apply)

  def apply(clientName: ClientName, instanceId: InstanceId, state: DirectoryServiceState): ClientServiceState = {
    ClientServiceState(clientName, InstanceServiceState(instanceId, state.serviceName, state.directory, state.state))
  }
}