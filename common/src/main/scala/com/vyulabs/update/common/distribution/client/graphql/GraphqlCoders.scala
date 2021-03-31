package com.vyulabs.update.common.distribution.client.graphql

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.{DistributionConsumerInfo, _}
import com.vyulabs.update.common.utils.JsonFormats.{FiniteDurationFormat, URLJsonFormat}
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.net.URL
import scala.concurrent.duration.FiniteDuration

// Queries

object PingCoder {
  def ping() = GraphqlQuery[String]("ping")
}

trait DistributionConsumersCoder {
  def getDistributionProvidersInfo(distributionName: Option[DistributionName] = None) =
    GraphqlQuery[Seq[DistributionProviderInfo]]("distributionProvidersInfo",
      distributionName.map(distributionName => GraphqlArgument("distribution" -> distributionName)).toSeq,
      subSelection = "{ distributionName, distributionUrl, uploadStateInterval }")

  def getDistributionConsumersInfo(distributionName: Option[DistributionName] = None) =
    GraphqlQuery[Seq[DistributionConsumerInfo]]("distributionConsumersInfo",
      distributionName.map(distributionName => GraphqlArgument("distribution" -> distributionName)).toSeq,
      subSelection = "{ distributionName, consumerProfile, testDistributionMatch }")

  def getDistributionProviderDesiredVersions(distributionName: DistributionName) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("distributionProviderDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distributionName)),
      "{ serviceName, version { distributionName, build } }")
}

trait DeveloperVersionsInfoCoder {
  def getDeveloperVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[DeveloperVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("developerVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version, "DeveloperVersionInput")).filter(_.value != JsNull),
      "{ serviceName, version { distributionName, build }, buildInfo { author, branches, date, comment } }")
}

trait ClientVersionsInfoCoder {
  def getClientVersionsInfo(serviceName: ServiceName, distributionName: Option[DistributionName] = None, version: Option[ClientVersion] = None) =
    GraphqlQuery[Seq[ClientVersionInfo]]("clientVersionsInfo",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("distribution" -> distributionName), GraphqlArgument("version" -> version, "ClientVersionInput")).filter(_.value != JsNull),
      "{ serviceName, version { distributionName, developerBuild, clientBuild }, buildInfo { author, branches, date, comment }, installInfo { user,  date} }")

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Seq[ServiceName]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("installedDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version { distributionName, developerBuild, clientBuild } }")
}

trait DeveloperDesiredVersionsCoder {
  def getDeveloperDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("developerDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version { distributionName, build } }")
}

trait ClientDesiredVersionsCoder {
  def getClientDesiredVersions(serviceNames: Seq[ServiceName] = Seq.empty) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> serviceNames, "[String!]")).filter(_.value != JsArray.empty),
      "{ serviceName, version { distributionName, developerBuild, clientBuild } }")
}

trait StateCoder {
  def getServiceStates(distributionName: Option[DistributionName] = None, serviceName: Option[ServiceName] = None, instanceId: Option[InstanceId] = None, directory: Option[ServiceDirectory] = None) =
    GraphqlQuery[Seq[DistributionServiceState]]("serviceStates",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName),
        GraphqlArgument("instance" -> instanceId), GraphqlArgument("directory" -> directory)).filter(_.value != JsNull),
      "{ distributionName instance { instanceId, serviceName, directory, service { date, installDate, startDate, version { distributionName, developerBuild, clientBuild }, updateToVersion { distributionName, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName], last: Option[Int]) =
    GraphqlQuery[Seq[DistributionFaultReport]]("faultReportsInfo",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName), GraphqlArgument("last" -> last, "Int")).filter(_.value != JsNull),
      "{ distributionName, report { faultId, info { date, instanceId, serviceDirectory, serviceName, serviceProfile, state { date, installDate, startDate, version { distributionName, developerBuild, clientBuild }, updateToVersion { distributionName, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode }, logTail }, files }}")
}

// Mutations

object LoginCoder {
  def login(userName: UserName, password: String) =
    GraphqlMutation[String]("login",
      Seq(GraphqlArgument("user" -> userName), GraphqlArgument("password" -> password)))
}

trait AddServiceLogsCoder {
  def addServiceLogs(serviceName: ServiceName, instanceId: InstanceId, processId: ProcessId, taskId: Option[TaskId],
                     serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation[Boolean]("addServiceLogs",
      Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("instance" -> instanceId), GraphqlArgument("process" -> processId),
        GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logs" -> logs, "[LogLineInput!]")) ++ taskId.map(taskId => GraphqlArgument("taskId" -> taskId)))
}

trait SetServiceStatesCoder {
  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation[Boolean]("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))
}

trait AddFaultReportInfoCoder {
  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation[Boolean]("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
}

trait UsersAdministrationCoder {
  def addUser(userName: UserName, roles: Seq[UserRole], password: String) =
    GraphqlMutation[Boolean]("addUser", Seq(GraphqlArgument("user" -> userName), GraphqlArgument("roles" -> roles, "[UserRole!]"), GraphqlArgument("password" -> password)))
  def removeUser(userName: UserName) =
    GraphqlMutation[Boolean]("removeUser", Seq(GraphqlArgument("user" -> userName)))
}

trait ConsumersAdministrationCoder {
  def addDistributionProvider(distributionName: DistributionName, distributionUrl: URL, uploadStateInterval: Option[FiniteDuration]) =
    GraphqlMutation[Boolean]("addDistributionProvider", Seq(GraphqlArgument("distribution" -> distributionName),
      GraphqlArgument("url" -> distributionUrl), GraphqlArgument("uploadStateInterval" -> uploadStateInterval.map(_.toJson.toString()), "[FiniteDuration!]")).filter(_.value != JsNull))
  def removeDistributionProvider(distributionName: DistributionName) =
    GraphqlMutation[Boolean]("removeDistributionProvider", Seq(GraphqlArgument("distribution" -> distributionName)))

  def installProviderVersion(distributionName: DistributionName, serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation[String]("installProviderVersion",
      Seq(GraphqlArgument("distribution" -> distributionName), GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version, "DeveloperDistributionVersionInput")))

  def addDistributionConsumerProfile(consumerProfile: ConsumerProfile, services: Seq[ServiceName]) =
    GraphqlMutation[Boolean]("addDistributionConsumerProfile", Seq(GraphqlArgument("profile" -> consumerProfile),
      GraphqlArgument("services" -> services, "[String!]")))
  def removeDistributionConsumerProfile(consumerProfile: ConsumerProfile) =
    GraphqlMutation[Boolean]("removeDistributionConsumerProfile", Seq(GraphqlArgument("profile" -> consumerProfile)))

  def addDistributionConsumer(distributionName: DistributionName, consumerProfile: ConsumerProfile, testDistributionMatch: Option[String]) =
    GraphqlMutation[Boolean]("addDistributionConsumer", Seq(GraphqlArgument("distribution" -> distributionName),
      GraphqlArgument("profile" -> consumerProfile), GraphqlArgument("testDistributionMatch" -> testDistributionMatch)).filter(_.value != JsNull))
  def removeDistributionConsumer(distributionName: DistributionName) =
    GraphqlMutation[Boolean]("removeDistributionConsumer", Seq(GraphqlArgument("distribution" -> distributionName)))
}

trait BuildDeveloperVersionCoder {
  def buildDeveloperVersion(serviceName: ServiceName, version: DeveloperVersion) =
    GraphqlMutation[String]("buildDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName), GraphqlArgument("version" -> version, "DeveloperVersionInput")))
}

trait RemoveDeveloperVersionCoder {
  def removeDeveloperVersion(serviceName: ServiceName, version: DeveloperDistributionVersion) =
    GraphqlMutation[Boolean]("removeDeveloperVersion", Seq(GraphqlArgument("service" -> serviceName),
      GraphqlArgument("version" -> version, "DeveloperDistributionVersionInput")))
}

trait BuildClientVersionCoder {
  def buildClientVersion(serviceName: ServiceName, developerVersion: DeveloperDistributionVersion, clientVersion: ClientDistributionVersion) =
    GraphqlMutation[String]("buildClientVersion", Seq(
      GraphqlArgument("service" -> serviceName),
      GraphqlArgument("developerVersion" -> developerVersion, "DeveloperDistributionVersionInput"),
      GraphqlArgument("clientVersion" -> clientVersion, "ClientDistributionVersionInput")))
}

trait RemoveClientVersionCoder {
  def removeClientVersion(serviceName: ServiceName, version: ClientDistributionVersion) =
    GraphqlMutation[Boolean]("removeClientVersion",
      Seq(GraphqlArgument("service" -> serviceName),
        GraphqlArgument("version" -> version, "ClientDistributionVersionInput")))
}

trait DesiredVersionsAdministrationCoder {
  def setDeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersionDelta]) =
    GraphqlMutation[Boolean]("setDeveloperDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionDeltaInput!]")))
  def setClientDesiredVersions(versions: Seq[ClientDesiredVersionDelta]) =
    GraphqlMutation[Boolean]("setClientDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionDeltaInput!]")))
}

trait SetTestedVersionsCoder {
  def setTestedVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation[Boolean]("setTestedVersions", Seq(GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionInput!]")))
}

trait SetInstalledDesiredVersionsCoder {
  def setInstalledDesiredVersions(versions: Seq[ClientDesiredVersion]) =
    GraphqlMutation[Boolean]("setInstalledDesiredVersions", Seq(GraphqlArgument("versions" -> versions, "[ClientDesiredVersionInput!]")))
}

trait AddDeveloperVersionInfoCoder {
  def addDeveloperVersionInfo(info: DeveloperVersionInfo) =
    GraphqlMutation[Boolean]("addDeveloperVersionInfo", Seq(GraphqlArgument("info" -> info, "DeveloperVersionInfoInput")))
}

trait AddClientVersionInfoCoder {
  def addClientVersionInfo(versionInfo: ClientVersionInfo) =
    GraphqlMutation[Boolean]("addClientVersionInfo", Seq(GraphqlArgument("info" -> versionInfo, "ClientVersionInfoInput")))
}

trait SubscribeTaskLogsCoder {
  def subscribeTaskLogs(taskId: TaskId) =
    GraphqlSubscription[SequencedServiceLogLine]("subscribeTaskLogs", Seq(GraphqlArgument("task" -> taskId, "String")),
      "{ sequence, logLine { distributionName, serviceName, taskId, instanceId, processId, directory, line { date, level, unit, message, terminationStatus } } }")
}

trait TestSubscriptionCoder {
  def testSubscription() =
    GraphqlSubscription[String]("testSubscription")
}

// Users

object DeveloperQueriesCoder extends DistributionConsumersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object DeveloperMutationsCoder extends BuildDeveloperVersionCoder with RemoveDeveloperVersionCoder
  with BuildClientVersionCoder with RemoveClientVersionCoder with DesiredVersionsAdministrationCoder {}
object DeveloperSubscriptionsCoder extends SubscribeTaskLogsCoder with TestSubscriptionCoder {}

object DeveloperGraphqlCoder {
  val developerQueries = DeveloperQueriesCoder
  val developerMutations = DeveloperMutationsCoder
  val developerSubscriptions = DeveloperSubscriptionsCoder
}

object AdministratorQueriesCoder extends DistributionConsumersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object AdministratorMutationsCoder extends UsersAdministrationCoder with ConsumersAdministrationCoder
  with RemoveDeveloperVersionCoder with RemoveClientVersionCoder with DesiredVersionsAdministrationCoder {}
object AdministratorSubscriptionsCoder extends SubscribeTaskLogsCoder {}

object AdministratorGraphqlCoder {
  val administratorQueries = AdministratorQueriesCoder
  val administratorMutations = AdministratorMutationsCoder
  val administratorSubscriptions = AdministratorSubscriptionsCoder
}

object DistributionQueriesCoder extends DeveloperVersionsInfoCoder with DeveloperDesiredVersionsCoder {}
object DistributionMutationsCoder extends SetServiceStatesCoder with AddFaultReportInfoCoder with SetTestedVersionsCoder with SetInstalledDesiredVersionsCoder {}

object DistributionGraphqlCoder {
  val distributionQueries = DistributionQueriesCoder
  val distributionMutations = DistributionMutationsCoder
}

object BuilderQueriesCoder extends DeveloperVersionsInfoCoder {}
object BuilderMutationsCoder extends AddDeveloperVersionInfoCoder with AddClientVersionInfoCoder {}
object BuilderSubscriptionsCoder extends SubscribeTaskLogsCoder {}

object BuilderGraphqlCoder {
  val builderQueries = BuilderQueriesCoder
  val builderMutations = BuilderMutationsCoder
  val builderSubscriptions = BuilderSubscriptionsCoder
}

object UpdaterQueriesCoder extends ClientDesiredVersionsCoder {}
object UpdaterMutationsCoder extends AddServiceLogsCoder with SetServiceStatesCoder with AddFaultReportInfoCoder {}

object UpdaterGraphqlCoder {
  val updaterQueries = UpdaterQueriesCoder
  val updaterMutations = UpdaterMutationsCoder
}