package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, InstanceId, ServiceDirectory, ServiceId}
import spray.json.DefaultJsonProtocol

case class DistributionInstanceState(distribution: DistributionId, instance: InstanceId,
                                     service: ServiceId, directory: ServiceDirectory, state: ServiceState)

object DistributionInstanceState extends DefaultJsonProtocol {
  implicit val clientServiceStateJson = jsonFormat5(DistributionInstanceState.apply)

  def apply(distribution: DistributionId, instance: InstanceId, state: DirectoryServiceState): DistributionInstanceState = {
    DistributionInstanceState(distribution, instance, state.service, state.directory, state.state)
  }
}