package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DistributionFaultReport, DistributionServiceState, InstanceServiceState, LogLine, ServiceFaultReport, ServiceState, UserInfo}
import com.vyulabs.update.version.{ClientVersion, DeveloperVersion}

import spray.json._
import spray.json.DefaultJsonProtocol._

trait GraphqlQueries {
  val httpClient: HttpClient

  def query[T](command: String, arguments: Map[String, JsValue] = Map.empty)(implicit reader: JsonReader[T]): Option[T] = {
    httpClient.graphqlRequest[T]("query", command, arguments)
  }
}

trait CommonQueries extends GraphqlQueries {
  def getUserInfo(): Option[UserInfo] = {
    query[UserInfo]("getUserInfo")
  }
}

trait AdministratorQueries extends CommonQueries {
  def getDeveloperVersionsInfo(serviceName: ServiceName, version: Option[DeveloperVersion]) : Option[Seq[DeveloperVersionInfo]] = {
    query[Seq[DeveloperVersionInfo]]("getDeveloperVersionsInfo",
      Map("service" -> JsString(serviceName), "version" -> version.toJson).filter(_._2 != JsNull))
  }

  def getDeveloperDesiredVersions(serviceName: Option[ServiceName]): Option[Seq[DeveloperDesiredVersion]] = {
    query[Seq[DeveloperDesiredVersion]]("getDeveloperDesiredVersions",
      Map("service" -> serviceName.toJson).filter(_._2 != JsNull))
  }

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[ClientVersion]) : Option[Seq[ClientVersionInfo]] = {
    query[Seq[ClientVersionInfo]]("getClientVersionsInfo",
      Map("service" -> JsString(serviceName), "version" -> version.toJson).filter(_._2 != JsNull))
  }

  def getClientDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    query[Seq[ClientDesiredVersion]]("getClientDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }

  def getDistributionClientsInfo(): Option[Seq[DistributionClientInfo]] = {
    query[Seq[DistributionClientInfo]]("getDistributionClientsInfo")
  }

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    query[Seq[ClientDesiredVersion]]("getInstalledDesiredVersions",
     Map("distribution" -> JsString(distributionName)) ++ Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }

  def getServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName], instanceId: Option[InstanceId],
                       directory: Option[ServiceDirectory]): Option[Seq[DistributionServiceState]] = {
    query[Seq[DistributionServiceState]]("getServicesState",
      Map("distribution" -> distributionName.toJson, "service" -> serviceName.toJson,
        "instance" -> instanceId.toJson, "directory" -> directory.toJson).filter(_._2 != JsNull))
  }

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]): Option[Seq[DistributionFaultReport]] = {
    query[Seq[DistributionFaultReport]]("getFaultReportsInfo",
      Map("distribution" -> distributionName.toJson, "service" -> serviceName.toJson, "last" -> last.toJson).filter(_._2 != JsNull))
  }
}

trait DistributionQueries extends CommonQueries {
  def getDistributionClientConfig(): Option[Seq[DistributionClientConfig]] = {
    query[Seq[DistributionClientConfig]]("getDistributionClientConfig")
  }

  def getDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[DeveloperDesiredVersion]] = {
    query[Seq[DeveloperDesiredVersion]]("getDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }
}

trait ServiceQueries extends CommonQueries {
  def getDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    query[Seq[ClientDesiredVersion]]("getDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }
}

trait GraphqlMutations {
  val httpClient: HttpClient

  def mutation(command: String, arguments: Map[String, JsValue] = Map.empty): Boolean = {
    httpClient.graphqlRequest[Boolean]("mutation", command, arguments).getOrElse(false)
  }
}

trait AdministratorMutations extends GraphqlMutations {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo): Boolean = {
    mutation("addDeveloperVersionInfo", Map("info" -> info.toJson))
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion): Boolean = {
    mutation("removeDeveloperVersion", Map("version" -> version.toJson))
  }

  def addClientVersionInfo(versionInfo: ClientVersionInfo): Boolean = {
    mutation("addClientVersionInfo", Map("info" -> versionInfo.toJson))
  }

  def removeClientVersion(serviceName: ServiceName, version: ClientVersion): Boolean = {
    mutation("removeClientVersion",
      Map("service" -> serviceName.toJson, "version" -> version.toJson))
  }

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]): Boolean = {
    mutation("setDeveloperDesiredVersions", Map("versions" -> versions.toJson))
  }

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean = {
    mutation("setClientDesiredVersions", Map("versions" -> versions.toJson))
  }
}

trait DistributionMutations extends GraphqlMutations {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]): Boolean = {
    mutation("setTestedVersions", Map("versions" -> versions.toJson))
  }

  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean = {
    mutation("setInstalledDesiredVersions", Map("versions" -> versions.toJson))
  }

  def setServiceStates(states: Seq[ServiceState]): Boolean = {
    mutation("setServiceStates", Map("states" -> states.toJson))
  }

  def addFaultReportInfo(fault: ServiceFaultReport): Boolean = {
    mutation("addFaultReportInfo", Map("fault" -> fault.toJson))
  }
}

trait ServiceMutations extends GraphqlMutations {
  def setServiceStates(states: Seq[ServiceState]): Boolean = {
    mutation("setServiceStates", Map("states" -> states.toJson))
  }

  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, serviceDirectory: ServiceDirectory, logLines: Seq[LogLine]): Boolean = {
    mutation("addServiceLogs",
      Map("service" -> serviceName.toJson, "instance" -> instanceId.toJson, "directory" -> serviceDirectory.toJson, "logLines" -> logLines.toJson))
  }

  def addFaultReportInfo(fault: ServiceFaultReport): Boolean = {
    mutation("addFaultReportInfo", Map("fault" -> fault.toJson))
  }
}