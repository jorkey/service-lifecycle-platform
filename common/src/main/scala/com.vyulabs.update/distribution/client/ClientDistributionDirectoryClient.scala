package com.vyulabs.update.distribution.client

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId, ProcessId, ServiceName, UpdaterInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.state.InstanceState
import com.vyulabs.update.info.{DesiredVersions, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.logs.{LogWriterInit, ServiceLogs}
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 25.04.19.
  * Copyright FanDate, Inc.
  */
class ClientDistributionDirectoryClient(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with ClientDistributionWebPaths {

  def downloadVersionsInfo(serviceName: ServiceName): Option[VersionsInfo] = {
    downloadToConfig(makeUrl(getDownloadVersionsInfoPath(serviceName))) match {
      case Some(config) =>
        Some(VersionsInfo(config))
      case None =>
        None
    }
  }

  def downloadDesiredVersions(): Option[DesiredVersions] = {
    val url = makeUrl(downloadDesiredVersionsPath)
    if (!exists(url)) {
      return None
    }
    downloadToConfig(url) match {
      case Some(config) =>
        Some(DesiredVersions(config))
      case None =>
        None
    }
  }

  def downloadInstanceState(instanceId: InstanceId, updaterProcessId: ProcessId): Option[InstanceState] = {
    val url = makeUrl(getDownloadInstanceStatePath(instanceId, updaterProcessId))
    if (!exists(url)) {
      return None
    }
    downloadToConfig(url) match {
      case Some(config) =>
        Some(InstanceState(config))
      case None =>
        None
    }
  }

  def uploadDesiredVersions(desiredVersions: DesiredVersions): Boolean = {
    uploadFromConfig(makeUrl(uploadDesiredVersionsPath), desiredVersionsName, uploadDesiredVersionsPath, desiredVersions.toConfig())
  }

  def uploadInstanceState(instanceId: InstanceId, updaterProcessId: ProcessId, instanceState: InstanceState): Boolean = {
    uploadFromConfig(makeUrl(getUploadInstanceStatePath(instanceId, updaterProcessId)), instanceStateName, uploadInstanceStatePath, instanceState.toConfig())
  }

  def uploadServiceLogs(instanceId: InstanceId, serviceInstanceName: ServiceInstanceName, serviceLogs: ServiceLogs): Boolean = {
    uploadFromConfig(makeUrl(getUploadServiceLogsPath(instanceId, serviceInstanceName)), serviceLogsName, uploadServiceLogsPath, serviceLogs.toConfig())
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
