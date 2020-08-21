package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{ServiceDirectory, ServiceName, InstanceId}
import com.vyulabs.update.distribution.DistributionWebPaths
import com.vyulabs.update.state.ProfiledServiceName

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait ClientDistributionWebPaths extends DistributionWebPaths {
  // New API paths

  val servicesStatePath = "services-state"
  val serviceLogsPath = "service-logs"

  // Old API paths

  val downloadInstanceStatePath = "download-instance-state"
  val downloadInstancesStatePath = "download-instances-state"

  val uploadInstanceStatePath = "upload-instance-state"
  val uploadServiceLogsPath = "upload-service-logs"

  // Names

  val servicesStateName = "services-state"
  val serviceLogsName = "service-logs"
  val serviceFaultName = "instance-fault"

  def getDownloadVersionsInfoPath(serviceName: ServiceName): String = {
    downloadVersionsInfoPath + "/" + encode(serviceName)
  }

  def getServicesStatePath(instanceId: InstanceId): String = {
    apiPathPrefix + "/" + servicesStatePath + "/" + encode(instanceId)
  }

  def getUploadServiceLogsPath(instanceId: InstanceId, profiledServiceName: ProfiledServiceName): String = {
    uploadServiceLogsPath + "/" + encode(instanceId) + "/" + encode(profiledServiceName.toString)
  }
}