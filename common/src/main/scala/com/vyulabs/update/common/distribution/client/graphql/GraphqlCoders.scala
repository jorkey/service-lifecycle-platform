package com.vyulabs.update.common.distribution.client.graphql

import com.vyulabs.update.common.common.Common.{DistributionName, InstanceId, ProcessId, ServiceDirectory, ServiceName, TaskId}
import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
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
  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, processId: ProcessId, taskId: Option[TaskId],
                     serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation[Boolean]("addServiceLogs",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("instance" -> instanceId), GraphqlArgument("process" -> processId),
        GraphqlArgument("taskId" -> taskId), GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logs" -> logs, "[LogLineInput!]")))
}

object CommonMutationsCoder extends CommonMutationsCoder

object AdministratorMutationsCoder extends CommonMutationsCoder {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo) =
    GraphqlMutation[Boolean]("addDeveloperVersionInfo", Seq(GraphqlArgument("info" -> info, "DeveloperVersionInfoInput")))

  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation[Boolean]("removeDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def addClientVersionInfo(versionInfo: ClientVersionInfo) =
    GraphqlMutation[Boolean]("addClientVersionInfo", Seq(GraphqlArgument("info" -> versionInfo, "ClientVersionInfoInput")))

  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion) =
    GraphqlMutation[Boolean]("removeClientVersion",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation[Boolean]("setDeveloperDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionInput!]")))

  def setClientDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation[Boolean]("setClientDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionInput!]")))
}

object DistributionMutationsCoder extends CommonMutationsCoder {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation[Boolean]("setTestedVersions", Seq(GraphqlArgument("versions" -> versions)))

  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation[Boolean]("setInstalledDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionInput!]")))

  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation[Boolean]("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation[Boolean]("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
}

object ServiceMutationsCoder extends CommonMutationsCoder {
  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation[Boolean]("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))

  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation[Boolean]("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
}

object AdministratorSubscriptionsCoder {
  def testSubscription() =
    GraphqlSubscription[String]("testSubscription")
}

  object AdministratorGraphqlCoder {
  val administratorQueries = AdministratorQueriesCoder
  val administratorMutations = AdministratorMutationsCoder
  val administratorSubscriptions = AdministratorSubscriptionsCoder
}

object DistributionGraphqlCoder {
  val distributionQueries = DistributionQueriesCoder
  val distributionMutations = DistributionMutationsCoder
}

object ServiceGraphqlCoder {
  val serviceMutations = ServiceMutationsCoder
  val serviceQueries = ServiceQueriesCoder
}