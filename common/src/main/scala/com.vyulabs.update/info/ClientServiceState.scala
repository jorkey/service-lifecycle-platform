package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{DistributionName, InstanceId}
import spray.json.DefaultJsonProtocol

case class ClientServiceState(distributionName: DistributionName, instance: InstanceServiceState)

object ClientServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat2(ClientServiceState.apply)

  def apply(distributionName: DistributionName, instanceId: InstanceId, state: DirectoryServiceState): ClientServiceState = {
    ClientServiceState(distributionName, InstanceServiceState(instanceId, state.serviceName, state.directory, state.state))
  }
}