package com.vyulabs.update.distribution.client

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId, ProcessId, ServiceName, UpdaterInstanceId}
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.state.InstanceState
import com.vyulabs.update.info.{DesiredVersions, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.logs.{ServiceLogs}
import org.slf4j.Logger
import spray.json._

import com.vyulabs.update.state.InstanceStateJson._
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.info.DesiredVersionsJson._
import com.vyulabs.update.logs.ServiceLogsJson._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 25.04.19.
  * Copyright FanDate, Inc.
  */
class ClientDistributionDirectoryClient(val url: URL)(implicit log: Logger) extends DistributionDirectoryClient(url)
    with ClientDistributionWebPaths {

  def downloadVersionsInfo(serviceName: ServiceName): Option[VersionsInfo] = {
    downloadToJson(makeUrl(getDownloadVersionsInfoPath(serviceName))) match {
      case Some(json) =>
        Some(json.convertTo[VersionsInfo])
      case None =>
        None
    }
  }

  def downloadDesiredVersions(): Option[DesiredVersions] = {
    val url = makeUrl(downloadDesiredVersionsPath)
    if (!exists(url)) {
      return None
    }
    downloadToJson(url) match {
      case Some(json) =>
        Some(json.convertTo[DesiredVersions])
      case None =>
        None
    }
  }

  def downloadInstanceState(instanceId: InstanceId, updaterProcessId: ProcessId): Option[InstanceState] = {
    val url = makeUrl(getDownloadInstanceStatePath(instanceId, updaterProcessId))
    if (!exists(url)) {
      return None
    }
    downloadToString(url) match {
      case Some(json) =>
        Some(json.parseJson.convertTo[InstanceState])
      case None =>
        None
    }
  }

  def uploadDesiredVersions(desiredVersions: DesiredVersions): Boolean = {
    uploadFromJson(makeUrl(uploadDesiredVersionsPath), desiredVersionsName, uploadDesiredVersionsPath, desiredVersions.toJson)
  }

  def uploadInstanceState(instanceId: InstanceId, updaterProcessId: ProcessId, instanceState: InstanceState): Boolean = {
    uploadFromString(makeUrl(getUploadInstanceStatePath(instanceId, updaterProcessId)), instanceStateName, uploadInstanceStatePath,
      instanceState.toJson.prettyPrint)
  }

  def uploadServiceLogs(instanceId: InstanceId, serviceInstanceName: ServiceInstanceName, serviceLogs: ServiceLogs): Boolean = {
    uploadFromJson(makeUrl(getUploadServiceLogsPath(instanceId, serviceInstanceName)), serviceLogsName, uploadServiceLogsPath, serviceLogs.toJson)
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
