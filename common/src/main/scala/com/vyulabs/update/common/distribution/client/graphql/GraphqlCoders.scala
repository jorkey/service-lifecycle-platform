package com.vyulabs.update.common.distribution.client.graphql

import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.SourceConfig
import com.vyulabs.update.common.info.{ClientDesiredVersion, ClientDesiredVersionDelta, ClientVersionInfo, DeveloperDesiredVersion, DeveloperDesiredVersionDelta, DeveloperVersionInfo, DistributionFaultReport, DistributionProviderInfo, DistributionServiceState, InstanceServiceState, LogLine, SequencedLogLine, SequencedServiceLogLine, ServiceFaultReport}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.common.utils.JsonFormats.FiniteDurationFormat
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol._
import spray.json._

import scala.concurrent.duration.FiniteDuration

// Queries

object PingCoder {
  def ping() = GraphqlQuery[String]("ping")
}

trait ServiceSourcesCoder {
  def getServiceSources(service: ServiceId) =
    GraphqlQuery[Seq[SourceConfig]]("serviceSources",
      Seq(GraphqlArgument("service" -> service)))
}

trait DistributionProvidersCoder {
  def getDistributionProvidersInfo(distribution: Option[DistributionId] = None) =
    GraphqlQuery[Seq[DistributionProviderInfo]]("providersInfo",
      distribution.map(distribution => GraphqlArgument("distribution" -> distribution)).toSeq,
      subSelection = "{ distribution, url, testConsumer, uploadStateIntervalSec }")

  def getDistributionProviderDesiredVersions(distribution: DistributionId) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("providerDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distribution)),
      "{ service, version { distribution, build } }")
}

trait DeveloperVersionsInfoCoder {
  def getDeveloperVersionsInfo(service: ServiceId, distribution: Option[DistributionId] = None, version: Option[DeveloperVersion] = None) =
    GraphqlQuery[Seq[DeveloperVersionInfo]]("developerVersionsInfo",
      Seq(GraphqlArgument("service" -> service), GraphqlArgument("distribution" -> distribution), GraphqlArgument("version" -> version, "DeveloperVersionInput")).filter(_.value != JsNull),
      "{ service, version { distribution, build }, buildInfo { author, sources { name, git { url, branch, cloneSubmodules } }, time, comment } }")
}

trait ClientVersionsInfoCoder {
  def getClientVersionsInfo(service: ServiceId, distribution: Option[DistributionId] = None, version: Option[ClientVersion] = None) =
    GraphqlQuery[Seq[ClientVersionInfo]]("clientVersionsInfo",
      Seq(GraphqlArgument("service" -> service), GraphqlArgument("distribution" -> distribution), GraphqlArgument("version" -> version, "ClientVersionInput")).filter(_.value != JsNull),
      "{ service, version { distribution, developerBuild, clientBuild }, buildInfo { author, sources { name, git { url, branch, cloneSubmodules } }, time, comment }, installInfo { account,  time} }")

  def getInstalledDesiredVersions(distribution: DistributionId, services: Seq[ServiceId]) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("installedDesiredVersions",
      Seq(GraphqlArgument("distribution" -> distribution), GraphqlArgument("services" -> services, "[String!]")).filter(_.value != JsArray.empty),
      "{ service, version { distribution, developerBuild, clientBuild } }")
}

trait DeveloperDesiredVersionsCoder {
  def getDeveloperDesiredVersions(services: Seq[ServiceId] = Seq.empty) =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("developerDesiredVersions",
      Seq(GraphqlArgument("services" -> services, "[String!]")).filter(_.value != JsArray.empty),
      "{ service, version { distribution, build } }")
}

trait ClientDesiredVersionsCoder {
  def getClientDesiredVersions(services: Seq[ServiceId] = Seq.empty) =
    GraphqlQuery[Seq[ClientDesiredVersion]]("clientDesiredVersions",
      Seq(GraphqlArgument("services" -> services, "[String!]")).filter(_.value != JsArray.empty),
      "{ service, version { distribution, developerBuild, clientBuild } }")
}

trait StateCoder {
  def getServiceStates(distribution: Option[DistributionId] = None, service: Option[ServiceId] = None, instance: Option[InstanceId] = None, directory: Option[ServiceDirectory] = None) =
    GraphqlQuery[Seq[DistributionServiceState]]("serviceStates",
      Seq(GraphqlArgument("distribution" -> distribution), GraphqlArgument("service" -> service),
        GraphqlArgument("instance" -> instance), GraphqlArgument("directory" -> directory)).filter(_.value != JsNull),
      "{ distribution instance { instance, service, directory, state { time, installTime, startTime, version { distribution, developerBuild, clientBuild }, updateToVersion { distribution, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distribution: Option[DistributionId], service: Option[ServiceId], last: Option[Int]) =
    GraphqlQuery[Seq[DistributionFaultReport]]("faultReports",
      Seq(GraphqlArgument("distribution" -> distribution), GraphqlArgument("service" -> service), GraphqlArgument("last" -> last, "Int")).filter(_.value != JsNull),
      "{ distribution, report { faultId, info { time, instance, service, serviceDirectory, serviceProfile, state { time, installTime, startTime, version { distribution, developerBuild, clientBuild }, updateToVersion { distribution, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode }, logTail }, files }}")
}

// Mutations

object LoginCoder {
  def login(account: AccountId, password: String) =
    GraphqlMutation[String]("login",
      Seq(GraphqlArgument("account" -> account), GraphqlArgument("password" -> password)))
}

trait SourcesAdministrationCoder {
  def addServiceSources(service: ServiceId, sources: Seq[SourceConfig]) =
    GraphqlMutation[Boolean]("addServiceSources", Seq(GraphqlArgument("service" -> service),
      GraphqlArgument("sources" -> sources, "[SourceConfigInput!]")))
  def changeServiceSources(service: ServiceId, sources: Seq[SourceConfig]) =
    GraphqlMutation[Boolean]("changeServiceSources", Seq(GraphqlArgument("service" -> service),
      GraphqlArgument("sources" -> sources, "[SourceConfigInput!]")))
  def removeServiceSources(service: ServiceId) =
    GraphqlMutation[Boolean]("removeServiceSources", Seq(GraphqlArgument("service" -> service)))
}

trait AccountsAdministrationCoder {
  def addAccount(account: AccountId, name: String, password: String, roles: Seq[AccountRole]) =
    GraphqlMutation[Boolean]("addAccount", Seq(GraphqlArgument("account" -> account),
      GraphqlArgument("name" -> name), GraphqlArgument("password" -> password), GraphqlArgument("roles" -> roles, "[AccountRole!]")))
  def removeAccount(account: AccountId) =
    GraphqlMutation[Boolean]("removeAccount", Seq(GraphqlArgument("account" -> account)))
}

trait ConsumersAdministrationCoder {
  def addServicesProfile(servicesProfile: ServicesProfileId, services: Seq[ServiceId]) =
    GraphqlMutation[Boolean]("addServicesProfile", Seq(GraphqlArgument("profile" -> servicesProfile),
      GraphqlArgument("services" -> services, "[String!]")))
  def removeServicesProfile(servicesProfile: ServicesProfileId) =
    GraphqlMutation[Boolean]("removeServicesProfile", Seq(GraphqlArgument("profile" -> servicesProfile)))

  def addProvider(distribution: DistributionId, distributionUrl: String, uploadStateInterval: Option[FiniteDuration]) =
    GraphqlMutation[Boolean]("addProvider", Seq(GraphqlArgument("distribution" -> distribution),
      GraphqlArgument("url" -> distributionUrl), GraphqlArgument("uploadStateInterval" -> uploadStateInterval.map(_.toJson.toString()), "[FiniteDuration!]")).filter(_.value != JsNull))
  def removeProvider(distribution: DistributionId) =
    GraphqlMutation[Boolean]("removeProvider", Seq(GraphqlArgument("distribution" -> distribution)))
}

trait BuildDeveloperVersionCoder {
  def buildDeveloperVersion(service: ServiceId, version: DeveloperVersion, sources: Seq[SourceConfig], comment: String) =
    GraphqlMutation[String]("buildDeveloperVersion", Seq(
      GraphqlArgument("service" -> service),
      GraphqlArgument("version" -> version, "DeveloperVersionInput"),
      GraphqlArgument("sources" -> sources, "[SourceConfigInput!]"),
      GraphqlArgument("comment" -> comment)))
}

trait RemoveDeveloperVersionCoder {
  def removeDeveloperVersion(service: ServiceId, version: DeveloperDistributionVersion) =
    GraphqlMutation[Boolean]("removeDeveloperVersion", Seq(GraphqlArgument("service" -> service),
      GraphqlArgument("version" -> version, "DeveloperDistributionVersionInput")))
}

trait BuildClientVersionCoder {
  def updateClientVersions(distribution: DistributionId, versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation[String]("updateClientVersions", Seq(
      GraphqlArgument("distribution" -> distribution),
      GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionInput!]")))
  def buildClientVersion(service: ServiceId, version: ClientDistributionVersion) =
    GraphqlMutation[String]("buildClientVersion", Seq(
      GraphqlArgument("service" -> service),
      GraphqlArgument("clientVersion" -> version, "ClientDistributionVersionInput")))
}

trait RemoveClientVersionCoder {
  def removeClientVersion(service: ServiceId, version: ClientDistributionVersion) =
    GraphqlMutation[Boolean]("removeClientVersion",
      Seq(GraphqlArgument("service" -> service),
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

trait AddServiceLogsCoder {
  def addServiceLogs(service: ServiceId, instance: InstanceId, processId: ProcessId, task: Option[TaskId],
                     serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation[Boolean]("addServiceLogs",
      Seq(GraphqlArgument("service" -> service), GraphqlArgument("instance" -> instance), GraphqlArgument("process" -> processId),
        GraphqlArgument("directory" -> serviceDirectory), GraphqlArgument("logs" -> logs, "[LogLineInput!]")) ++ task.map(task => GraphqlArgument("task" -> task)))
}

trait SetServiceStatesCoder {
  def setServiceStates(states: Seq[InstanceServiceState]) =
    GraphqlMutation[Boolean]("setServiceStates", Seq(GraphqlArgument("states" -> states, "[InstanceServiceStateInput!]")))
}

trait AddFaultReportInfoCoder {
  def addFaultReportInfo(fault: ServiceFaultReport) =
    GraphqlMutation[Boolean]("addFaultReportInfo", Seq(GraphqlArgument("fault" -> fault, "ServiceFaultReportInput")))
}


trait SubscribeTaskLogsCoder {
  def subscribeTaskLogs(task: TaskId) =
    GraphqlSubscription[SequencedLogLine]("subscribeTaskLogs", Seq(GraphqlArgument("task" -> task, "String")),
      "{ sequence, line { time, level, unit, message, terminationStatus } }")
}

trait TestSubscriptionCoder {
  def testSubscription() =
    GraphqlSubscription[String]("testSubscription")
}

// Accounts

object DeveloperQueriesCoder extends DistributionProvidersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object DeveloperMutationsCoder extends BuildDeveloperVersionCoder with RemoveDeveloperVersionCoder
  with BuildClientVersionCoder with RemoveClientVersionCoder with DesiredVersionsAdministrationCoder {}
object DeveloperSubscriptionsCoder extends SubscribeTaskLogsCoder with TestSubscriptionCoder {}

object DeveloperGraphqlCoder {
  val developerQueries = DeveloperQueriesCoder
  val developerMutations = DeveloperMutationsCoder
  val developerSubscriptions = DeveloperSubscriptionsCoder
}

object AdministratorQueriesCoder extends DistributionProvidersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object AdministratorMutationsCoder extends SourcesAdministrationCoder with AccountsAdministrationCoder with ConsumersAdministrationCoder with BuildClientVersionCoder
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