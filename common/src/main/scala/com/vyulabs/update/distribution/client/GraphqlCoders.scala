package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info.{ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DistributionFaultReport, DistributionServiceState, InstanceServiceState, LogLine, ServiceFaultReport, ServiceState, UserInfo}
import com.vyulabs.update.version.{ClientVersion, DeveloperVersion}
import spray.json._
import spray.json.DefaultJsonProtocol._

trait CommonQueriesCoder {
  def getUserInfo() =
    GraphqlQuery[UserInfo]("getUserInfo")
}

object AdministratorQueriesCoder extends CommonQueriesCoder {
  def getDeveloperVersionsInfo(serviceName: ServiceName, version: Option[DeveloperVersion]) =
    GraphqlQueryList[DeveloperVersionInfo]("developerVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment } }")

  def getDeveloperDesiredVersions(serviceName: Option[ServiceName]) =
    GraphqlQueryList[DeveloperDesiredVersion]("developerDesiredVersions",
      Seq(GraphqlArgument("service" -> serviceName)).filter(_.value != JsNull),
      "{ serviceName, version }")

  def getClientVersionsInfo(serviceName: ServiceName, version: Option[ClientVersion]) =
    GraphqlQueryList[ClientVersionInfo]("clientVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment }, installInfo { user,  date} }")

  def getClientDesiredVersions(serviceNames: Seq[ServiceName]) =
    GraphqlQueryList[ClientDesiredVersion]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames)).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getDistributionClientsInfo() =
    GraphqlQueryList[DistributionClientInfo]("distributionClientsInfo",
      subSelection = "{ distributionName, clientConfig { installProfile, testDistributionMatch } }")

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]) =
    GraphqlQueryList[ClientDesiredVersion]("installedDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("services" -> serviceNames)).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getServiceStates(distributionName: Option[DistributionName], serviceName: Option[ServiceName], instanceId: Option[InstanceId], directory: Option[ServiceDirectory]) =
    GraphqlQueryList[DistributionServiceState]("serviceStates",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName),
        GraphqlArgument("instance" -> instanceId), GraphqlArgument("directory" -> directory)).filter(_.value != JsNull),
      "{ distributionName instance { instanceId, serviceName, directory, service { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]) =
    GraphqlQueryList[DistributionFaultReport]("faultReportsInfo",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName), GraphqlArgument("last" -> last)).filter(_.value != JsNull),
      "{ distributionName, report { faultId, info { date, instanceId, serviceDirectory, serviceName, serviceProfile, state { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode }, logTail } }}")
}

object DistributionQueriesCoder extends CommonQueriesCoder {
  def getDistributionClientConfig() =
    GraphqlQueryList[DistributionClientConfig]("distributionClientConfig",
      subSelection =  "{ installProfile, testDistributionMatch }")

  def getDesiredVersions(serviceNames: Seq[ServiceName]) =
    GraphqlQueryList[DeveloperDesiredVersion]("desiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames)).filter(_.value != JsArray.empty),
      "{ serviceName, version }")
}

object ServiceQueriesCoder extends CommonQueriesCoder {
  def getDesiredVersions(serviceNames: Seq[ServiceName]) =
    GraphqlQueryList[ClientDesiredVersion]("desiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames)).filter(_.value != JsArray.empty),
     "{ serviceName, version }")
}

object AdministratorMutationsCoder {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo) =
    GraphqlMutation("addDeveloperVersionInfo", Seq(GraphqlArgument("info" -> info, "DeveloperVersionInfoInput")))

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion) =
    GraphqlMutation("removeDeveloperVersion", Seq(GraphqlArgument("version" -> version)))

  def addClientVersionInfo(versionInfo: ClientVersionInfo) =
    GraphqlMutation("addClientVersionInfo", Seq(GraphqlArgument("info" -> versionInfo)))

  def removeClientVersion(serviceName: ServiceName, version: ClientVersion) =
    GraphqlMutation("removeClientVersion",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation("setDeveloperDesiredVersions", Seq(GraphqlArgument("versions" -> versions)))

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation("setClientDesiredVersions", Seq(GraphqlArgument("versions" -> versions)))
}

object DistributionMutationsCoder {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation("setTestedVersions", Seq(GraphqlArgument("versions" -> versions)))

  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation("setInstalledDesiredVersions", Seq(GraphqlArgument("versions" -> versions)))

  def setServiceStates(states: Seq[ServiceState]) =
    GraphqlMutation("setServiceStates", Seq(GraphqlArgument("states" -> states)))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault)))
}

object ServiceMutationsCoder {
  def setServiceStates(states: Seq[ServiceState]) =
    GraphqlMutation("setServiceStates", Seq(GraphqlArgument("states" -> states)))

  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, serviceDirectory: ServiceDirectory, logLines: Seq[LogLine]) =
    GraphqlMutation("addServiceLogs",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("instance" -> instanceId),
          GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logLines" -> logLines)))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault)))
}

object AdministratorGraphqlCoder {
  val administratorQueries = AdministratorQueriesCoder
  val administratorMutations = AdministratorMutationsCoder
}

object DistributionGraphqlCoder {
  val distributionQueries = DistributionQueriesCoder
  val distributionMutations = DistributionMutationsCoder
}

object ServiceGraphqlCoder {
  val serviceMutations = ServiceMutationsCoder
  val serviceQueries = ServiceQueriesCoder
}