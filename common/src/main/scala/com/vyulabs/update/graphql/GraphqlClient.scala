package com.vyulabs.update.graphql

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DistributionFaultReport, DistributionServiceState, InstanceServiceState, LogLine, ServiceFaultReport, ServiceState, UserInfo}
import com.vyulabs.update.version.{ClientVersion, DeveloperVersion}

trait CommonQueries {
  def getUserInfo(): UserInfo
}

trait AdministratorQueries extends CommonQueries {
  def getDeveloperVersionsInfo(serviceName: ServiceName, version: Option[DeveloperVersion]) : Seq[DeveloperVersionInfo]
  def getDeveloperDesiredVersions(serviceName: Option[ServiceName]): Seq[DeveloperDesiredVersion]

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[ClientVersion]) : Seq[ClientVersionInfo]
  def getClientDesiredVersions(serviceNames: Seq[ServiceName]): Seq[ClientDesiredVersion]

  def getDistributionClientsInfo(): Seq[DistributionClientInfo]
  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]): Seq[ClientDesiredVersion]
  def getServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName], instanceId: Option[InstanceId],
                       directory: Option[ServiceDirectory]): Seq[DistributionServiceState]
  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]): Seq[DistributionFaultReport]
}

trait DistributionQueries extends CommonQueries {
  def getDistributionClientConfig(): Seq[DistributionClientConfig]
  def getDesiredVersions(serviceNames: Seq[ServiceName]): Seq[DeveloperDesiredVersion]
}

trait ServiceQueries extends CommonQueries {
  def getDesiredVersions(serviceNames: Seq[ServiceName]): Seq[ClientDesiredVersion]
}

trait AdministratorMutations {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo): Boolean
  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion): Boolean
  def addClientVersionInfo(versionInfo: ClientVersionInfo): Boolean
  def removeClientVersion(serviceName: ServiceName, version: ClientVersion): Boolean
  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]): Boolean
  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean
}

trait DistributionMutations {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]): Boolean
  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean
  def setServiceStates(states: Seq[ServiceState]): Boolean
  def addFaultReportInfo(fault: ServiceFaultReport): Boolean
}

trait ServiceMutations {
  def setServiceStates(states: Seq[ServiceState]): Boolean
  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, serviceDirectory: ServiceDirectory, logLines: Seq[LogLine]): Boolean
  def addFaultReportInfo(fault: ServiceFaultReport): Boolean
}