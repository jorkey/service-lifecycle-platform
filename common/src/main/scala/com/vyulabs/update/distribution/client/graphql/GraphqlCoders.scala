package com.vyulabs.update.distribution.client.graphql

import com.vyulabs.update.common.Common.{DistributionName, InstanceId, ProcessId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info._
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import spray.json.DefaultJsonProtocol._
import spray.json._

trait CommonQueriesCoder {
  def getUserInfo() =
    GraphqlQuery[UserInfo]("getUserInfo")
}

object AdministratorQueriesCoder extends CommonQueriesCoder {
  def getDeveloperVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[DeveloperDistributionVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("developerVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment } }")

  def getDeveloperDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("developerDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getClientVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[ClientDistributionVersion] = None) =
    GraphqlQuery[Seq[ClientVersionInfo]]("clientVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment }, installInfo { user,  date} }")

  def getClientDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getDistributionClientsInfo() =
    GraphqlQuery[Seq[DistributionClientInfo]]("distributionClientsInfo",
      subSelection = "{ distributionName, clientConfig { installProfile, testDistributionMatch } }")

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("installedDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getServiceStates(distributionName: Option[DistributionName], serviceName: Option[ServiceName], instanceId: Option[InstanceId], directory: Option[ServiceDirectory]) =
    GraphqlQuery[Seq[DistributionServiceState]]("serviceStates",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName),
        GraphqlArgument("instance" -> instanceId), GraphqlArgument("directory" -> directory)).filter(_.value != JsNull),
      "{ distributionName instance { instanceId, serviceName, directory, service { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]) =
    GraphqlQuery[Seq[DistributionFaultReport]]("faultReportsInfo",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName), GraphqlArgument("last" -> last, "Int")).filter(_.value != JsNull),
      "{ distributionName, report { faultId, info { date, instanceId, serviceDirectory, serviceName, serviceProfile, state { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode }, logTail }, files }}")
}

object DistributionQueriesCoder extends CommonQueriesCoder {
  def getDistributionClientConfig() =
    GraphqlQuery[DistributionClientConfig]("distributionClientConfig",
      subSelection =  "{ installProfile, testDistributionMatch }")

  def getVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[DeveloperDistributionVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("versionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment } }")

  def getDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("desiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")
}

object ServiceQueriesCoder extends CommonQueriesCoder {
  def getDesiredVersions(serviceNames: Seq[ServiceName]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("desiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
     "{ serviceName, version }")
}

trait CommonMutationsCoder {
  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, processId: ProcessId,
                     serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation("addServiceLogs",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("instance" -> instanceId), GraphqlArgument("process" -> processId),
        GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logs" -> logs, "[LogLineInput!]")))
}

object CommonMutationsCoder extends CommonMutationsCoder

object AdministratorMutationsCoder extends CommonMutationsCoder {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo) =
    GraphqlMutation("addDeveloperVersionInfo", Seq(GraphqlArgument("info" -> info, "DeveloperVersionInfoInput")))

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation("removeDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def addClientVersionInfo(versionInfo: ClientVersionInfo) =
    GraphqlMutation("addClientVersionInfo", Seq(GraphqlArgument("info" -> versionInfo, "ClientVersionInfoInput")))

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion) =
    GraphqlMutation("removeClientVersion",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation("setDeveloperDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionInput!]")))

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation("setClientDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionInput!]")))
}

object DistributionMutationsCoder extends CommonMutationsCoder {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation("setTestedVersions", Seq(GraphqlArgument("versions" -> versions)))

  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation("setInstalledDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionInput!]")))

  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
}

object ServiceMutationsCoder extends CommonMutationsCoder {
  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
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