package com.vyulabs.update.common.distribution.client.graphql

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConsumerInfo
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.JsonFormats.FiniteDurationFormat
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.URL
import scala.concurrent.duration.FiniteDuration

trait CommonQueriesCoder {
  def getUserInfo() =
    GraphqlQuery[UserInfo]("getUserInfo")
}

object AdministratorQueriesCoder extends CommonQueriesCoder {
  def getDeveloperVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[DeveloperVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("developerVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment } }")

  def getDeveloperDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("developerDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getClientVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[ClientVersion] = None) =
    GraphqlQuery[Seq[ClientVersionInfo]]("clientVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment }, installInfo { user,  date} }")

  def getClientDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getDistributionClientsInfo() =
    GraphqlQuery[Seq[DistributionConsumerInfo]]("distributionClientsInfo",
      subSelection = "{ distributionName, clientConfig { installProfile, testDistributionMatch } }")

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("installedDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")

  def getServiceStates(distributionName: Option[DistributionName] = None, serviceName: Option[ServiceName] = None, instanceId: Option[InstanceId] = None, directory: Option[ServiceDirectory] = None) =
    GraphqlQuery[Seq[DistributionServiceState]]("serviceStates",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName),
        GraphqlArgument("instance" -> instanceId), GraphqlArgument("directory" -> directory)).filter(_.value != JsNull),
      "{ distributionName instance { instanceId, serviceName, directory, service { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]) =
    GraphqlQuery[Seq[DistributionFaultReport]]("faultReportsInfo",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName), GraphqlArgument("last" -> last, "Int")).filter(_.value != JsNull),
      "{ distributionName, report { faultId, info { date, instanceId, serviceDirectory, serviceName, serviceProfile, state { date, installDate, startDate, version, updateToVersion, updateError { critical, error }, failuresCount, lastExitCode }, logTail }, files }}")

  def getProviderDeveloperDesiredVersions() =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("getProviderDeveloperDesiredVersions", Seq.empty)
}

object DistributionQueriesCoder extends CommonQueriesCoder {
  def getDistributionConsumerInfo() =
    GraphqlQuery[DistributionConsumerInfo]("distributionConsumerInfo",
      subSelection =  "{ distributionName, installProfile, testDistributionMatch }")

  def getVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[DeveloperDistributionVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("versionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version)).filter(_.value != JsNull),
      "{ serviceName, version, buildInfo { author, branches, date, comment } }")

  def getDeveloperDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("developerDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version }")
}

object ServiceQueriesCoder extends CommonQueriesCoder {
  def getClientDesiredVersions(serviceNames: Seq[ServiceName]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
     "{ serviceName, version }")
}

trait CommonMutationsCoder {
  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, processId: ProcessId, taskId: Option[TaskId],
                     serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation[Boolean]("addServiceLogs",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("instance" -> instanceId), GraphqlArgument("process" -> processId),
        GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logs" -> logs, "[LogLineInput!]")) ++ taskId.map(taskId => GraphqlArgument("taskId" -> taskId)))
}

object CommonMutationsCoder extends CommonMutationsCoder

object AdministratorMutationsCoder extends CommonMutationsCoder {
  def addUser(userName: UserName, role: UserRole, password: String) =
    GraphqlMutation[Boolean]("addUser", Seq(GraphqlArgument("user" -> userName), GraphqlArgument("role" -> role, "UserRole"), GraphqlArgument("password" -> password)))

  def buildDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion) =
    GraphqlMutation[String]("buildDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))
  def addDeveloperVersionInfo(info: DeveloperVersionInfo) =
    GraphqlMutation[Boolean]("addDeveloperVersionInfo", Seq(GraphqlArgument("info" -> info, "DeveloperVersionInfoInput")))
  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation[Boolean]("removeDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def buildClientVersion(serviceName: ServiceName, developerVersion: DeveloperDistributionVersion, clientVersion: ClientDistributionVersion) =
    GraphqlMutation[String]("buildClientVersion", Seq(GraphqlArgument("service" -> serviceName),
      GraphqlArgument("developerVersion" -> developerVersion), GraphqlArgument("clientVersion" -> clientVersion)))
  def addClientVersionInfo(versionInfo: ClientVersionInfo) =
    GraphqlMutation[Boolean]("addClientVersionInfo", Seq(GraphqlArgument("info" -> versionInfo, "ClientVersionInfoInput")))
  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion) =
    GraphqlMutation[Boolean]("removeClientVersion",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersionDelta]) =
    GraphqlMutation[Boolean]("setDeveloperDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionDeltaInput!]")))
  def setClientDesiredVersions(versions: Seq[ClientDesiredVersionDelta]) =
    GraphqlMutation[Boolean]("setClientDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionDeltaInput!]")))

  def addDistributionProvider(distributionName: DistributionName, distributionUrl: URL, uploadStateInterval: Option[FiniteDuration]) =
    GraphqlMutation[Boolean]("addDistributionProvider", Seq(GraphqlArgument("distribution" -> distributionName),
      GraphqlArgument("url" -> distributionUrl.toString, "[URL!]"), GraphqlArgument("uploadStateInterval" -> uploadStateInterval.map(_.toJson.toString()), "[FiniteDuration!]")))

  def installProviderVersion(serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation[String]("installProviderDeveloperVersion",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version)))

  def addDistributionConsumer(distributionName: DistributionName, installProfile: ProfileName, testDistributionMatch: Option[String]) =
    GraphqlMutation[Boolean]("addDistributionConsumer", Seq(GraphqlArgument("distribution" -> distributionName),
      GraphqlArgument("profile" -> installProfile), GraphqlArgument("testDistributionMatch" -> testDistributionMatch)))
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
  def subscribeTaskLogs(taskId: TaskId) =
    GraphqlSubscription[SequencedServiceLogLine]("subscribeTaskLogs", Seq(GraphqlArgument("task" -> taskId, "String")),
      "{ sequence, logLine { distributionName, serviceName, taskId, instanceId, processId, directory, line { date, level, unit, message, terminationStatus } } }")

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