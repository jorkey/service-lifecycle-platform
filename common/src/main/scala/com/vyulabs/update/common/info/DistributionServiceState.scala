package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId}
import spray.json.DefaultJsonProtocol

case class DistributionServiceState(distributionName: DistributionName, instance: InstanceServiceState)

object DistributionServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat2(DistributionServiceState.apply)

  def apply(distributionName: DistributionName, instanceId: InstanceId, state: DirectoryServiceState): DistributionServiceState = {
    DistributionServiceState(distributionName, InstanceServiceState(instanceId, state.serviceName, state.directory, state.state))
  }
}