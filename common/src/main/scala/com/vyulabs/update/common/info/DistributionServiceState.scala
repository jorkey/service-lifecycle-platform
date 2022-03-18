package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId, ServiceDirectory, ServiceId}
import spray.json.DefaultJsonProtocol

case class DistributionServiceState(distribution: DistributionId, instance: InstanceId,
                                    service: ServiceId, directory: ServiceDirectory, state: ServiceState)

object DistributionServiceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat5(DistributionServiceState.apply)

  def apply(distribution: DistributionId, instance: InstanceId, state: DirectoryServiceState): DistributionServiceState = {
    DistributionServiceState(distribution, instance, state.service, state.directory, state.state)
  }
}