package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DistributionFaultReport, DistributionServiceState, InstanceServiceState, LogLine, ServiceFaultReport, ServiceState, UserInfo}
import com.vyulabs.update.version.{ClientVersion, DeveloperVersion}

import spray.json._
import spray.json.DefaultJsonProtocol._

trait GraphqlQueriesClient {
  protected val client: HttpClient

  def query[T](command: String, arguments: Map[String, JsValue] = Map.empty)(implicit reader: JsonReader[T]): Option[T] = {
    client.graphqlRequest[T]("query", command, arguments)
  }
}

trait CommonQueriesClient {
  protected val client: GraphqlQueriesClient

  def getUserInfo(): Option[UserInfo] = {
    client.query[UserInfo]("getUserInfo")
  }
}

trait GraphqlClient extends GraphqlQueriesClient with GraphqlMutationsClient

trait AdministratorQueriesClient {
  protected val client: GraphqlQueriesClient

  def getDeveloperVersionsInfo(serviceName: ServiceName, version: Option[DeveloperVersion]) : Option[Seq[DeveloperVersionInfo]] = {
    client.query[Seq[DeveloperVersionInfo]]("getDeveloperVersionsInfo",
      Map("service" -> JsString(serviceName), "version" -> version.toJson).filter(_._2 != JsNull))
  }

  def getDeveloperDesiredVersions(serviceName: Option[ServiceName]): Option[Seq[DeveloperDesiredVersion]] = {
    client.query[Seq[DeveloperDesiredVersion]]("getDeveloperDesiredVersions",
      Map("service" -> serviceName.toJson).filter(_._2 != JsNull))
  }

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[ClientVersion]) : Option[Seq[ClientVersionInfo]] = {
    client.query[Seq[ClientVersionInfo]]("getClientVersionsInfo",
      Map("service" -> JsString(serviceName), "version" -> version.toJson).filter(_._2 != JsNull))
  }

  def getClientDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    client.query[Seq[ClientDesiredVersion]]("getClientDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }

  def getDistributionClientsInfo(): Option[Seq[DistributionClientInfo]] = {
    client.query[Seq[DistributionClientInfo]]("getDistributionClientsInfo")
  }

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    client.query[Seq[ClientDesiredVersion]]("getInstalledDesiredVersions",
     Map("distribution" -> JsString(distributionName)) ++ Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }

  def getServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName], instanceId: Option[InstanceId],
                       directory: Option[ServiceDirectory]): Option[Seq[DistributionServiceState]] = {
    client.query[Seq[DistributionServiceState]]("getServicesState",
      Map("distribution" -> distributionName.toJson, "service" -> serviceName.toJson,
        "instance" -> instanceId.toJson, "directory" -> directory.toJson).filter(_._2 != JsNull))
  }

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]): Option[Seq[DistributionFaultReport]] = {
    client.query[Seq[DistributionFaultReport]]("getFaultReportsInfo",
      Map("distribution" -> distributionName.toJson, "service" -> serviceName.toJson, "last" -> last.toJson).filter(_._2 != JsNull))
  }
}

trait DistributionQueriesClient {
  protected val client: GraphqlQueriesClient

  def getDistributionClientConfig(): Option[Seq[DistributionClientConfig]] = {
    client.query[Seq[DistributionClientConfig]]("getDistributionClientConfig")
  }

  def getDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[DeveloperDesiredVersion]] = {
    client.query[Seq[DeveloperDesiredVersion]]("getDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }
}

trait ServiceQueriesClient {
  protected val client: GraphqlQueriesClient

  def getDesiredVersions(serviceNames: Seq[ServiceName]): Option[Seq[ClientDesiredVersion]] = {
    client.query[Seq[ClientDesiredVersion]]("getDesiredVersions",
      Map("services" -> serviceNames).filter(!_._2.isEmpty).mapValues(_.toJson))
  }
}

trait GraphqlMutationsClient {
  protected val client: HttpClient

  def mutation(command: String, arguments: Map[String, JsValue] = Map.empty): Boolean = {
    client.graphqlRequest[Boolean]("mutation", command, arguments).getOrElse(false)
  }
}

trait AdministratorMutationsClient {
  protected val client: GraphqlMutationsClient

  def addDeveloperVersionInfo(info: DeveloperVersionInfo): Boolean = {
    client.mutation("addDeveloperVersionInfo", Map("info" -> info.toJson))
  }

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion): Boolean = {
    client.mutation("removeDeveloperVersion", Map("version" -> version.toJson))
  }

  def addClientVersionInfo(versionInfo: ClientVersionInfo): Boolean = {
    client.mutation("addClientVersionInfo", Map("info" -> versionInfo.toJson))
  }

  def removeClientVersion(serviceName: ServiceName, version: ClientVersion): Boolean = {
    client.mutation("removeClientVersion",
      Map("service" -> serviceName.toJson, "version" -> version.toJson))
  }

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]): Boolean = {
    client.mutation("setDeveloperDesiredVersions", Map("versions" -> versions.toJson))
  }

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean = {
    client.mutation("setClientDesiredVersions", Map("versions" -> versions.toJson))
  }
}

trait DistributionMutationsClient {
  protected val client: GraphqlMutationsClient

  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]): Boolean = {
    client.mutation("setTestedVersions", Map("versions" -> versions.toJson))
  }

  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]): Boolean = {
    client.mutation("setInstalledDesiredVersions", Map("versions" -> versions.toJson))
  }

  def setServiceStates(states: Seq[ServiceState]): Boolean = {
    client.mutation("setServiceStates", Map("states" -> states.toJson))
  }

  def addFaultReportInfo(fault: ServiceFaultReport): Boolean = {
    client.mutation("addFaultReportInfo", Map("fault" -> fault.toJson))
  }
}

trait ServiceMutationsClient {
  protected val client: GraphqlMutationsClient

  def setServiceStates(states: Seq[ServiceState]): Boolean = {
    client.mutation("setServiceStates", Map("states" -> states.toJson))
  }

  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, serviceDirectory: ServiceDirectory, logLines: Seq[LogLine]): Boolean = {
    client.mutation("addServiceLogs",
      Map("service" -> serviceName.toJson, "instance" -> instanceId.toJson, "directory" -> serviceDirectory.toJson, "logLines" -> logLines.toJson))
  }

  def addFaultReportInfo(fault: ServiceFaultReport): Boolean = {
    client.mutation("addFaultReportInfo", Map("fault" -> fault.toJson))
  }
}