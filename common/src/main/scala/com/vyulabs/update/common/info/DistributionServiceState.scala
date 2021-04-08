package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId}
import spray.json.DefaultJsonProtocol

case class DistributionServiceState(distribution: DistributionName, instance: InstanceServiceState)

object DistributionServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat2(DistributionServiceState.apply)

  def apply(distribution: DistributionName, instance: InstanceId, state: DirectoryServiceState): DistributionServiceState = {
    DistributionServiceState(distribution, InstanceServiceState(instance, state.service, state.directory, state.state))
  }
}