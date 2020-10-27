package com.vyulabs.update.distribution.client

import java.io.File
import java.net.URL

import com.vyulabs.update.common.Common.{InstanceId, ServiceName}
import com.vyulabs.update.info.{DesiredVersion, DirectoryServiceState, ProfiledServiceName, VersionsInfo}
import com.vyulabs.update.distribution.DistributionDirectoryClient
import com.vyulabs.update.logs.ServiceLogs
import com.vyulabs.update.info.VersionsInfoJson._
import com.vyulabs.update.logs.ServiceLogs._
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger
import spray.json._

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

  def downloadDesiredVersions(): Option[Map[ServiceName, BuildVersion]] = {
    val url = makeUrl(downloadDesiredVersionsPath)
    if (!exists(url)) {
      return None
    }
    downloadToJson(url) match {
      case Some(json) =>
        Some(json.convertTo[Seq[DesiredVersion]].foldLeft(Map.empty[ServiceName, BuildVersion])((map, entry) =>
          map + (entry.serviceName -> entry.buildVersion)))
      case None =>
        None
    }
  }

  def downloadServicesState(instanceId: InstanceId): Option[Seq[DirectoryServiceState]] = {
    val url = makeUrl(getServicesStatePath(instanceId))
    if (!exists(url)) {
      return None
    }
    downloadToString(url) match {
      case Some(json) =>
        Some(json.parseJson.convertTo[Seq[DirectoryServiceState]])
      case None =>
        None
    }
  }

  def uploadDesiredVersions(desiredVersions: Seq[DesiredVersion]): Boolean = {
    // TODO graphql
    //uploadFromJson(makeUrl(uploadDesiredVersionsPath), desiredVersionsName, uploadDesiredVersionsPath, desiredVersions.toJson)
    false
  }

  def uploadServicesStates(servicesState: Seq[DirectoryServiceState]): Boolean = {
    // TODO graphql
    false
  }

  def uploadServiceLogs(instanceId: InstanceId, profiledServiceName: ProfiledServiceName, serviceLogs: ServiceLogs): Boolean = {
    uploadFromJson(makeUrl(getUploadServiceLogsPath(instanceId, profiledServiceName)), serviceLogsName, uploadServiceLogsPath, serviceLogs.toJson)
  }

  def uploadServiceFault(serviceName: ServiceName, faultFile: File): Boolean = {
    uploadFromFile(makeUrl(getUploadServiceFaultPath(serviceName)), serviceFaultName, faultFile)
  }
}
