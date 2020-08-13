package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{ProcessId, ServiceName, UpdaterDirectory, VmInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.distribution.DistributionWebPaths

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 23.04.19.
  * Copyright FanDate, Inc.
  */
trait ClientDistributionWebPaths extends DistributionWebPaths {
  // New API paths

  val instanceStatePath = "instance-state"
  val serviceLogsPath = "service-logs"

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
    downloadVersionsInfoPath + "/" + encode(serviceName)
  }

  def getDownloadInstanceStatePath(instanceId: VmInstanceId, updaterDirectory: UpdaterDirectory, updaterProcessId: ProcessId): String = {
    downloadInstanceStatePath + "/" + encode(instanceId) + "/" + encode(updaterDirectory) + "/" + encode(updaterProcessId)
  }

  def getUploadInstanceStatePath(instanceId: VmInstanceId, updaterDirectory: UpdaterDirectory, updaterProcessId: ProcessId): String = {
    apiPathPrefix + "/" + instanceStatePath + "/" + encode(instanceId) + "/" + encode(updaterDirectory) + "/" + encode(updaterProcessId)
  }

  def getUploadServiceLogsPath(instanceId: VmInstanceId, serviceInstanceName: ServiceInstanceName): String = {
    uploadServiceLogsPath + "/" + encode(instanceId) + "/" + encode(serviceInstanceName.toString)
  }
}