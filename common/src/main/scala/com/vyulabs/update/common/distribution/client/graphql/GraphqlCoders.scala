package com.vyulabs.update.common.distribution.client.graphql

import com.vyulabs.update.common.accounts.{ConsumerAccountProperties, UserAccountProperties}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.{BuildServiceConfig, NamedStringValue, Repository}
import com.vyulabs.update.common.info.AccountRole.AccountRole
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import spray.json.DefaultJsonProtocol._
import spray.json._

// Queries

object PingCoder {
  def ping() = GraphqlQuery[String]("ping")
}

trait ServicesConfigCoder {
  def getDeveloperServicesConfig() =
    GraphqlQuery[Seq[BuildServiceConfig]]("buildDeveloperServicesConfig",
      Seq.empty,
      "{ service, sources { name, git { url, branch, cloneSubmodules } }, environment { name, value } }")

  def getClientServicesConfig() =
    GraphqlQuery[Seq[BuildServiceConfig]]("buildClientServicesConfig",
      Seq.empty,
      "{ service, environment { name, value } }")
}

trait DistributionProvidersCoder {
  def getDistributionProvidersInfo(distribution: Option[DistributionId] = None) =
    GraphqlQuery[Seq[DistributionProviderInfo]]("providersInfo",
      distribution.map(distribution => GraphqlArgument("distribution" -> distribution)).toSeq,
      subSelection = "{ distribution, url, accessToken, testConsumer, uploadState, autoUpdate }")

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
  def getTestedVersions() =
    GraphqlQuery[Seq[DeveloperDesiredVersion]]("testedVersions",
      Seq.empty,
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
      "{ distribution payload { instance, service, directory, state { time, installTime, startTime, version { distribution, developerBuild, clientBuild }, updateToVersion { distribution, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode } } }"
    )

  def getFaultReportsInfo(distribution: Option[DistributionId], service: Option[ServiceId], limit: Option[Int]) =
    GraphqlQuery[Seq[DistributionFaultReport]]("faults",
      Seq(GraphqlArgument("distribution" -> distribution), GraphqlArgument("service" -> service), GraphqlArgument("limit" -> limit, "Int")).filter(_.value != JsNull),
      "{ distribution, payload { fault, info { time, instance, service, serviceDirectory, serviceRole, state { time, installTime, startTime, version { distribution, developerBuild, clientBuild }, updateToVersion { distribution, developerBuild, clientBuild }, updateError { critical, error }, failuresCount, lastExitCode }, logTail { time, level, unit, message, terminationStatus } }, files { path, length } }}")
}

// Mutations

object LoginCoder {
  def login(account: AccountId, password: String) =
    GraphqlMutation[String]("login",
      Seq(GraphqlArgument("account" -> account), GraphqlArgument("password" -> password)))
}

trait ServiceConfigsAdministrationCoder {
  def setBuildDeveloperServiceConfig(service: ServiceId,
                                     distribution: Option[DistributionId],
                                     environment: Seq[NamedStringValue],
                                     repositories: Seq[Repository], macroValues: Seq[NamedStringValue]) = {
    GraphqlMutation[Boolean]("setBuildDeveloperServiceConfig", Seq(
      GraphqlArgument("service" -> service),
      GraphqlArgument("distribution" -> distribution),
      GraphqlArgument("environment" -> environment, "[NamedStringValueInput!]"),
      GraphqlArgument("repositories" -> repositories, "[RepositoryInput!]"),
      GraphqlArgument("macroValues" -> macroValues, "[NamedStringValueInput!]")
    ))
  }

  def removeBuildDeveloperServiceConfig(service: ServiceId) = {
    GraphqlMutation[Boolean]("removeBuildDeveloperServiceConfig", Seq(
      GraphqlArgument("service" -> service)))
  }

  def setBuildClientServiceConfig(service: ServiceId,
                                  distribution: Option[DistributionId],
                                  environment: Seq[NamedStringValue],
                                  repositories: Seq[Repository], macroValues: Seq[NamedStringValue]) = {
    GraphqlMutation[Boolean]("setBuildClientServiceConfig", Seq(
      GraphqlArgument("service" -> service),
      GraphqlArgument("distribution" -> distribution),
      GraphqlArgument("environment" -> environment, "[NamedStringValueInput!]"),
      GraphqlArgument("repositories" -> repositories, "[RepositoryInput!]"),
      GraphqlArgument("macroValues" -> macroValues, "[NamedStringValueInput!]")
    ))
  }

  def removeBuildClientServiceConfig(service: ServiceId) = {
    GraphqlMutation[Boolean]("removeBuildClientServiceConfig", Seq(
      GraphqlArgument("service" -> service)))
  }

}

trait AccountsAdministrationCoder {
  def addUserAccount(account: AccountId, name: String, role: AccountRole, password: String, properties: UserAccountProperties) =
    GraphqlMutation[Boolean]("addUserAccount", Seq(
      GraphqlArgument("account" -> account),
      GraphqlArgument("name" -> name),
      GraphqlArgument("role" -> role, "AccountRole"),
      GraphqlArgument("password" -> password),
      GraphqlArgument("properties" -> properties, "UserAccountPropertiesInput")))

  def addServiceAccount(account: AccountId, name: String, role: AccountRole) =
    GraphqlMutation[Boolean]("addServiceAccount", Seq(
      GraphqlArgument("account" -> account),
      GraphqlArgument("name" -> name),
      GraphqlArgument("role" -> role, "AccountRole")))

  def addConsumerAccount(account: AccountId, name: String, consumer: ConsumerAccountProperties) =
    GraphqlMutation[Boolean]("addConsumerAccount", Seq(
      GraphqlArgument("account" -> account),
      GraphqlArgument("name" -> name),
      GraphqlArgument("properties" -> consumer, "ConsumerAccountPropertiesInput")))

  def removeAccount(account: AccountId) =
    GraphqlMutation[Boolean]("removeAccount", Seq(GraphqlArgument("account" -> account)))
}

trait ConsumersAdministrationCoder {
  def addServicesProfile(profile: ServicesProfileId, services: Seq[ServiceId]) =
    GraphqlMutation[Boolean]("addServicesProfile", Seq(GraphqlArgument("profile" -> profile),
      GraphqlArgument("services" -> services, "[String!]")))
  def removeServicesProfile(profile: ServicesProfileId) =
    GraphqlMutation[Boolean]("removeServicesProfile", Seq(GraphqlArgument("profile" -> profile)))

  def addProvider(distribution: DistributionId, distributionUrl: String, accessToken: String,
                  testConsumer: Option[String], uploadState: Option[Boolean], autoUpdate: Option[Boolean]) =
    GraphqlMutation[Boolean]("addProvider", Seq(GraphqlArgument("distribution" -> distribution),
      GraphqlArgument("url" -> distributionUrl), GraphqlArgument("accessToken" -> accessToken),
      GraphqlArgument("testConsumer" -> testConsumer),
      GraphqlArgument("uploadState" -> uploadState),
      GraphqlArgument("autoUpdate" -> autoUpdate)
    ).filter(_.value != JsNull))
  def removeProvider(distribution: DistributionId) =
    GraphqlMutation[Boolean]("removeProvider", Seq(GraphqlArgument("distribution" -> distribution)))
}

trait BuildDeveloperVersionCoder {
  def buildDeveloperVersion(service: ServiceId, version: DeveloperVersion,
                            comment: String, buildClientVersion: Boolean) =
    GraphqlMutation[String]("buildDeveloperVersion", Seq(
      GraphqlArgument("service" -> service),
      GraphqlArgument("version" -> version, "DeveloperVersionInput"),
      GraphqlArgument("comment" -> comment),
      GraphqlArgument("buildClientVersion" -> buildClientVersion)
    ))
}

trait RemoveDeveloperVersionCoder {
  def removeDeveloperVersion(service: ServiceId, version: DeveloperDistributionVersion) =
    GraphqlMutation[Boolean]("removeDeveloperVersion", Seq(GraphqlArgument("service" -> service),
      GraphqlArgument("version" -> version, "DeveloperDistributionVersionInput")))
}

trait BuildClientVersionCoder {
  def buildClientVersions(versions: Seq[DeveloperDesiredVersion]) =
    GraphqlMutation[String]("buildClientVersions", Seq(
      GraphqlArgument("versions" -> versions, "[DeveloperDesiredVersionInput!]")))
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

trait AddLogsCoder {
  def addLogs(service: ServiceId, instance: InstanceId, process: ProcessId, task: Option[TaskId],
              serviceDirectory: ServiceDirectory, logs: Seq[LogLine]) =
    GraphqlMutation[Boolean]("addLogs",
      Seq(GraphqlArgument("service" -> service), GraphqlArgument("instance" -> instance), GraphqlArgument("process" -> process),
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


trait SubscribeLogsCoder {
  def subscribeTaskLogs(task: TaskId) =
    GraphqlSubscription[Seq[SequencedServiceLogLine]]("subscribeLogs", Seq(GraphqlArgument("task" -> task, "String")),
      "{ sequence, instance, directory, process, payload { time, level, unit, message, terminationStatus } }")
}

trait TestSubscriptionCoder {
  def testSubscription() =
    GraphqlSubscription[String]("testSubscription")
}

// Accounts

object DeveloperQueriesCoder extends ServicesConfigCoder with DistributionProvidersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object DeveloperMutationsCoder extends BuildDeveloperVersionCoder with RemoveDeveloperVersionCoder
  with BuildClientVersionCoder with RemoveClientVersionCoder with DesiredVersionsAdministrationCoder {}
object DeveloperSubscriptionsCoder extends SubscribeLogsCoder with TestSubscriptionCoder {}

object DeveloperGraphqlCoder {
  val developerQueries = DeveloperQueriesCoder
  val developerMutations = DeveloperMutationsCoder
  val developerSubscriptions = DeveloperSubscriptionsCoder
}

object AdministratorQueriesCoder extends ServicesConfigCoder with DistributionProvidersCoder with DeveloperVersionsInfoCoder with ClientVersionsInfoCoder
  with DeveloperDesiredVersionsCoder with ClientDesiredVersionsCoder with StateCoder {}
object AdministratorMutationsCoder extends ServiceConfigsAdministrationCoder with AccountsAdministrationCoder with ConsumersAdministrationCoder
  with AddDeveloperVersionInfoCoder with AddClientVersionInfoCoder  with BuildClientVersionCoder
  with RemoveDeveloperVersionCoder with RemoveClientVersionCoder with DesiredVersionsAdministrationCoder {}
object AdministratorSubscriptionsCoder extends SubscribeLogsCoder {}

object AdministratorGraphqlCoder {
  val administratorQueries = AdministratorQueriesCoder
  val administratorMutations = AdministratorMutationsCoder
  val administratorSubscriptions = AdministratorSubscriptionsCoder
}

object ConsumerQueriesCoder extends DeveloperVersionsInfoCoder with DeveloperDesiredVersionsCoder {}
object ConsumerMutationsCoder extends SetServiceStatesCoder with AddFaultReportInfoCoder with SetTestedVersionsCoder with SetInstalledDesiredVersionsCoder {}

object ConsumerGraphqlCoder {
  val distributionQueries = ConsumerQueriesCoder
  val distributionMutations = ConsumerMutationsCoder
}

object BuilderQueriesCoder extends DeveloperVersionsInfoCoder with ClientDesiredVersionsCoder {}
object BuilderMutationsCoder extends AddDeveloperVersionInfoCoder with AddClientVersionInfoCoder {}
object BuilderSubscriptionsCoder extends SubscribeLogsCoder {}

object BuilderGraphqlCoder {
  val builderQueries = BuilderQueriesCoder
  val builderMutations = BuilderMutationsCoder
  val builderSubscriptions = BuilderSubscriptionsCoder
}

object UpdaterQueriesCoder extends ClientDesiredVersionsCoder {}
object UpdaterMutationsCoder extends AddLogsCoder with SetServiceStatesCoder with AddFaultReportInfoCoder {}

object UpdaterGraphqlCoder {
  val updaterQueries = UpdaterQueriesCoder
  val updaterMutations = UpdaterMutationsCoder
}