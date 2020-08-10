package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{InstanceId, ProcessId, ServiceName, UpdaterInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.distribution.DistributionWebPaths

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait ClientDistributionWebPaths extends DistributionWebPaths {
  // New API paths

  val getInstanceStatePath = "get-instance-state"
  val getInstancesStatePath = "get-instances-state"

  val putInstanceStatePath = "put-instance-state"
  val putServiceLogsPath = "put-service-logs"

  // Old API paths

  val downloadInstanceStatePath = "download-instance-state"
  val downloadInstancesStatePath = "download-instances-state"

  val uploadInstanceStatePath = "upload-instance-state"
  val uploadServiceLogsPath = "upload-service-logs"

  // Names

  val instanceStateName = "instance-state"
  val serviceLogsName = "service-logs"
  val serviceFaultName = "instance-fault"

  def getDownloadVersionsInfoPath(serviceName: ServiceName): String = {
    downloadVersionsInfoPath + "/" + serviceName
  }

  def getDownloadInstanceStatePath(instanceId: InstanceId, updaterProcessId: ProcessId): String = {
    downloadInstanceStatePath + "/" + instanceId + "/" + updaterProcessId
  }

  def getUploadInstanceStatePath(instanceId: InstanceId, updaterProcessId: ProcessId): String = {
    uploadInstanceStatePath + "/" + instanceId + "/" + updaterProcessId
  }

  def getUploadServiceLogsPath(instanceId: InstanceId, serviceInstanceName: ServiceInstanceName): String = {
    uploadServiceLogsPath + "/" + instanceId + "/" + serviceInstanceName.toString
  }
}