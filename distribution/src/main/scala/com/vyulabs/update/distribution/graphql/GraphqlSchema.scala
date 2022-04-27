package com.vyulabs.update.distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.accounts.{AccountInfo, ConsumerAccountInfo}
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, AccountRole}
import com.vyulabs.update.distribution.graphql.GraphqlTypes._
import com.vyulabs.update.distribution.graphql.utils._
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger
import sangria.marshalling.sprayJson._
import sangria.schema._
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext

case class GraphqlWorkspace(config: DistributionConfig, collections: DatabaseCollections, directory: DistributionDirectory, taskManager: TaskManager)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext)
    extends ConfigBuilderUtils with DistributionInfoUtils with ServiceProfilesUtils with DistributionProvidersUtils with DistributionConsumersUtils
      with DeveloperVersionUtils with ClientVersionUtils with StateUtils with LogUtils with FaultsUtils with TasksUtils
      with RunBuilderUtils with AccountsUtils {
  protected val configBuilderUtils = this
  protected val distributionInfoUtils = this
  protected val serviceProfilesUtils = this
  protected val distributionProvidersUtils = this
  protected val distributionConsumersUtils = this
  protected val developerVersionUtils = this
  protected val clientVersionUtils = this
  protected val stateUtils = this
  protected val logUtils = this
  protected val faultsUtils = this
  protected val tasksUtils = this
  protected val runBuilderUtils = this
  protected val accountsUtils = this
}

case class GraphqlContext(accessToken: Option[AccessToken], accountInfo: Option[AccountInfo], workspace: GraphqlWorkspace)

object GraphqlSchema {
  // Arguments

  val AccountArg = Argument("account", StringType)
  val UserAccountPropertiesArg = Argument("properties", UserAccountPropertiesInputType)
  val ConsumerAccountPropertiesArg = Argument("properties", ConsumerAccountPropertiesInputType)
  val NameArg = Argument("name", StringType)
  val OldPasswordArg = Argument("oldPassword", StringType)
  val PasswordArg = Argument("password", StringType)
  val AccountRoleArg = Argument("role", AccountRoleType)
  val DistributionArg = Argument("distribution", StringType)
  val InstanceArg = Argument("instance", StringType)
  val ProcessArg = Argument("process", StringType)
  val TaskArg = Argument("task", StringType)
  val DirectoryArg = Argument("directory", StringType)
  val ServiceArg = Argument("service", StringType)
  val ServicesArg = Argument("services", ListInputType(StringType))
  val ProfileArg = Argument("profile", StringType)
  val DeveloperVersionArg = Argument("version", DeveloperVersionInputType)
  val ClientVersionArg = Argument("version", ClientVersionInputType)
  val DeveloperDistributionVersionArg = Argument("version", DeveloperDistributionVersionInputType)
  val ClientDistributionVersionArg = Argument("version", ClientDistributionVersionInputType)
  val DeveloperVersionInfoArg = Argument("info", DeveloperVersionInfoInputType)
  val ClientVersionInfoArg = Argument("info", ClientVersionInfoInputType)
  val DeveloperDesiredVersionsArg = Argument("versions", ListInputType(DeveloperDesiredVersionInputType))
  val ClientDesiredVersionsArg = Argument("versions", ListInputType(ClientDesiredVersionInputType))
  val DeveloperDesiredVersionDeltasArg = Argument("versions", ListInputType(DeveloperDesiredVersionDeltaInputType))
  val ClientDesiredVersionDeltasArg = Argument("versions", ListInputType(ClientDesiredVersionDeltaInputType))
  val ServiceStatesArg = Argument("states", ListInputType(ServiceStateInputType))
  val InstanceServiceStatesArg = Argument("states", ListInputType(InstanceServiceStateInputType))
  val LogLinesArg = Argument("logs", ListInputType(LogLineInputType))
  val ServiceFaultReportArg = Argument("fault", ServiceFaultReportInputType)
  val ArgumentsArg = Argument("arguments", ListInputType(StringType))
  val AccessTokenArg = Argument("accessToken", StringType)
  val UrlArg = Argument("url", StringType)
  val CommentArg = Argument("comment", StringType)
  val DownloadUpdatesArg = Argument("downloadUpdates", BooleanType)
  val RebuildWithNewConfigArg = Argument("rebuildWithNewConfig", BooleanType)
  val LimitArg = Argument("limit", IntType)
  val EnvironmentArg = Argument("environment", ListInputType(NamedStringValueInputType))
  val MacroValuesArg = Argument("macroValues", ListInputType(NamedStringValueInputType))
  val RepositoriesArg = Argument("repositories", ListInputType(RepositoryInputType))
  val PrivateFilesArg = Argument("privateFiles", ListInputType(FileInfoInputType))
  val FileArg = Argument("file", StringType)

  val OptionAccountArg = Argument("account", OptionInputType(StringType))
  val OptionNameArg = Argument("name", OptionInputType(StringType))
  val OptionOldPasswordArg = Argument("oldPassword", OptionInputType(StringType))
  val OptionPasswordArg = Argument("password", OptionInputType(StringType))
  val OptionAccountRoleArg = Argument("role", OptionInputType(AccountRoleType))
  val OptionUserAccountPropertiesArg = Argument("properties", OptionInputType(UserAccountPropertiesInputType))
  val OptionConsumerAccountPropertiesArg = Argument("properties", OptionInputType(ConsumerAccountPropertiesInputType))
  val OptionEmailArg = Argument("email", OptionInputType(StringType))
  val OptionNotificationsArg = Argument("notifications", OptionInputType(ListInputType(StringType)))
  val OptionTaskArg = Argument("task", OptionInputType(StringType))
  val OptionProfileArg = Argument("profile", OptionInputType(StringType))
  val OptionDistributionArg = Argument("distribution", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionProcessArg = Argument("process", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionDeveloperVersionArg = Argument("version", OptionInputType(DeveloperVersionInputType))
  val OptionClientVersionArg = Argument("version", OptionInputType(ClientVersionInputType))
  val OptionMergedArg = Argument("merged", OptionInputType(BooleanType))
  val OptionFromArg = Argument("from", OptionInputType(BigintType))
  val OptionToArg = Argument("to", OptionInputType(BigintType))
  val OptionFromTimeArg = Argument("fromTime", OptionInputType(DateType))
  val OptionToTimeArg = Argument("toTime", OptionInputType(DateType))
  val OptionLevelsArg = Argument("levels", OptionInputType(ListInputType(StringType)))
  val OptionUnitArg = Argument("unit", OptionInputType(StringType))
  val OptionFindArg = Argument("find", OptionInputType(StringType))
  val OptionFaultArg = Argument("fault", OptionInputType(StringType))
  val OptionTypeArg = Argument("type", OptionInputType(StringType))
  val OptionParametersArg = Argument("parameters", OptionInputType(ListInputType(TaskParameterInputType)))
  val OptionLimitArg = Argument("limit", OptionInputType(IntType))
  val OptionPrefetchArg = Argument("prefetch", OptionInputType(IntType))
  val OptionUploadStateArg = Argument("uploadState", OptionInputType(BooleanType))
  val OptionAutoUpdateArg = Argument("autoUpdate", OptionInputType(BooleanType))
  val OptionTestConsumerArg = Argument("testConsumer", OptionInputType(StringType))
  val OptionBuildClientVersionArg = Argument("buildClientVersion", OptionInputType(BooleanType))
  val OptionOnlyActiveArg = Argument("onlyActive", OptionInputType(BooleanType))
  val OptionEnvironmentArg = Argument("environment", OptionInputType(ListInputType(NamedStringValueInputType)))

  // Queries

  def Queries(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Query",
    fields[GraphqlContext, Unit](
      // Check alive
      Field("ping", StringType,
        resolve = _ => { "pong" }),

      // Distribution server info
      Field("distributionInfo", DistributionInfoType,
        resolve = c => { c.ctx.workspace.getDistributionInfo() }),

      // Own account operations
      Field("whoAmI", UserAccountInfoType,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.whoAmI(c.ctx.accessToken.get.account) }),

      // Accounts
      Field("accountsList", ListType(StringType),
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getAccountsInfo().map(_.map(_.account)) }),
      Field("userAccountsInfo", ListType(UserAccountInfoType),
        arguments = OptionAccountArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getUserAccountsInfo(c.arg(OptionAccountArg)) }),
      Field("serviceAccountsInfo", ListType(ServiceAccountInfoType),
        arguments = OptionAccountArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceAccountsInfo(c.arg(OptionAccountArg)) }),
      Field("consumerAccountsInfo", ListType(ConsumerAccountInfoType),
        arguments = OptionAccountArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getConsumerAccountsInfo(c.arg(OptionAccountArg)) }),
      Field("accessToken", StringType,
        arguments = AccountArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getAccessToken(c.arg(AccountArg)) }),

      // Build config
      Field("buildDeveloperServicesConfig", ListType(BuildServiceConfigType),
        arguments = OptionServiceArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getBuildDeveloperServicesConfig(c.arg(OptionServiceArg)) }),
      Field("buildClientServicesConfig", ListType(BuildServiceConfigType),
        arguments = OptionServiceArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getBuildClientServicesConfig(c.arg(OptionServiceArg)) }),

      // Profiles
      Field("serviceProfiles", ListType(ServicesProfileType),
        arguments = OptionProfileArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceProfiles(c.arg(OptionProfileArg)) }),

      // Developer versions
      Field("developerVersionsInfo", ListType(DeveloperVersionInfoType),
        arguments = OptionServiceArg :: OptionDistributionArg :: OptionDeveloperVersionArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.DistributionConsumer, AccountRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInfo(c.arg(OptionServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("developerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionTestConsumerArg :: OptionServicesArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.Builder, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => {
          if (c.ctx.accountInfo.get.role == AccountRole.DistributionConsumer) {
            val accountInfo = c.ctx.accountInfo.get.asInstanceOf[ConsumerAccountInfo]
            c.ctx.workspace.getDeveloperDesiredVersionsByConsumer(accountInfo.properties.profile,
              c.arg(OptionTestConsumerArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet)
          } else {
            c.ctx.workspace.getDeveloperDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet)
          }
        }),
      Field("developerDesiredVersionsHistory", ListType(TimedDeveloperDesiredVersionsType),
        arguments = LimitArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => {
          c.ctx.workspace.getDeveloperDesiredVersionsHistory(c.arg(LimitArg))
        }),

      // Tested versions
      Field("testedVersions", OptionType(ListType(DeveloperDesiredVersionType)),
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => {
          c.ctx.workspace.getTestedVersions(
            Some(if (c.ctx.accountInfo.get.role == AccountRole.DistributionConsumer) c.ctx.accessToken.get.account else c.ctx.workspace.config.distribution))
        }),

      // Client versions
      Field("clientVersionsInfo", ListType(ClientVersionInfoType),
        arguments = OptionServiceArg :: OptionDistributionArg :: OptionClientVersionArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getClientVersionsInfo(c.arg(OptionServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionClientVersionArg)) }),
      Field("clientDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.Builder, AccountRole.Updater) :: Nil,
        resolve = c => { c.ctx.workspace.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("clientDesiredVersionsHistory", ListType(TimedClientDesiredVersionsType),
        arguments = LimitArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => {
          c.ctx.workspace.getClientDesiredVersionsHistory(c.arg(LimitArg))
        }),

      // Distribution providers
      Field("providersInfo", ListType(ProviderInfoType),
        arguments = OptionDistributionArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getProvidersInfo(c.arg(OptionDistributionArg)) }),
      Field("providerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = DistributionArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getProviderDesiredVersions(c.arg(DistributionArg)) }),
      Field("providerTestedVersions", ListType(DeveloperDesiredVersionType),
        arguments = DistributionArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getProviderTestedVersions(c.arg(DistributionArg)) }),

      // Distribution consumers
      Field("installedDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = DistributionArg :: OptionServicesArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getConsumerInstalledDesiredVersions(c.arg(DistributionArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      // State
      Field("serviceStates", ListType(DistributionServiceStateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.Updater) :: Nil,
        resolve = c => { c.ctx.workspace.getServicesState(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),

      // Logs
      Field("logServices", ListType(StringType),
        resolve = c => { c.ctx.workspace.getLogServices() }),
      Field("logInstances", ListType(StringType),
        arguments = ServiceArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogInstances(c.arg(ServiceArg)) }),
      Field("logDirectories", ListType(StringType),
        arguments = ServiceArg :: InstanceArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogDirectories(service = c.arg(ServiceArg),
          instance = c.arg(InstanceArg)) }),
      Field("logProcesses", ListType(StringType),
        arguments = ServiceArg :: InstanceArg :: DirectoryArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogProcesses(service = c.arg(ServiceArg),
          instance = c.arg(InstanceArg), directory = c.arg(DirectoryArg)) }),
      Field("logLevels", ListType(StringType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: OptionProcessArg :: OptionTaskArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogLevels(service = c.arg(OptionServiceArg), instance = c.arg(OptionInstanceArg),
          directory = c.arg(OptionDirectoryArg), process = c.arg(OptionProcessArg), task = c.arg(OptionTaskArg)) }),
      Field("logsStartTime", OptionType(DateType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: OptionProcessArg :: OptionTaskArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogsStartTime(service = c.arg(OptionServiceArg), instance = c.arg(OptionInstanceArg),
          directory = c.arg(OptionDirectoryArg), process = c.arg(OptionProcessArg), task = c.arg(OptionTaskArg)) }),
      Field("logsEndTime", OptionType(DateType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: OptionProcessArg :: OptionTaskArg :: Nil,
        resolve = c => { c.ctx.workspace.getLogsEndTime(service = c.arg(OptionServiceArg), instance = c.arg(OptionInstanceArg),
          directory = c.arg(OptionDirectoryArg), process = c.arg(OptionProcessArg), task = c.arg(OptionTaskArg)) }),
      Field("logs", ListType(SequencedServiceLogLineType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: OptionProcessArg :: OptionTaskArg ::
          OptionLevelsArg :: OptionFindArg :: OptionFromTimeArg :: OptionToTimeArg ::
          OptionFromArg :: OptionToArg :: OptionLimitArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getLogs(service = c.arg(OptionServiceArg),
          instance = c.arg(OptionInstanceArg), directory = c.arg(OptionDirectoryArg), process = c.arg(OptionProcessArg),
          task = c.arg(OptionTaskArg), levels = c.arg(OptionLevelsArg), find = c.arg(OptionFindArg),
          fromTime = c.arg(OptionFromTimeArg), toTime = c.arg(OptionToTimeArg),
          from = c.arg(OptionFromArg), to = c.arg(OptionToArg), limit = c.arg(OptionLimitArg)) }),
      // Faults
      Field("faultDistributions", ListType(StringType),
        resolve = c => { c.ctx.workspace.getFaultDistributions() }),
      Field("faultServices", ListType(StringType),
        arguments = OptionDistributionArg :: Nil,
        resolve = c => { c.ctx.workspace.getFaultServices(c.arg(OptionDistributionArg)) }),
      Field("faultsStartTime", OptionType(DateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: Nil,
        resolve = c => { c.ctx.workspace.getFaultsStartTime(c.arg(OptionDistributionArg), c.arg(OptionServiceArg)) }),
      Field("faultsEndTime", OptionType(DateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: Nil,
        resolve = c => { c.ctx.workspace.getFaultsEndTime(c.arg(OptionDistributionArg), c.arg(OptionServiceArg)) }),
      Field("faults", ListType(DistributionFaultReportType),
        arguments = OptionFaultArg :: OptionDistributionArg :: OptionServiceArg ::
          OptionFromTimeArg :: OptionToTimeArg :: OptionLimitArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getFaults(
          distribution = c.arg(OptionDistributionArg), fault = c.arg(OptionFaultArg), service = c.arg(OptionServiceArg),
          fromTime = c.arg(OptionFromTimeArg), toTime = c.arg(OptionToTimeArg), limit = c.arg(OptionLimitArg)) }),

      // Tasks
      Field("taskTypes", ListType(StringType),
        resolve = c => { c.ctx.workspace.getTaskTypes() }),
      Field("taskServices", ListType(StringType),
        resolve = c => { c.ctx.workspace.getTaskServices() }),
      Field("tasks", ListType(SequencedTaskInfoType),
        arguments = OptionTaskArg :: OptionTypeArg :: OptionParametersArg :: OptionServiceArg ::
          OptionOnlyActiveArg :: OptionFromTimeArg :: OptionToTimeArg :: OptionFromArg :: OptionLimitArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getTasks(
          task = c.arg(OptionTaskArg), taskType = c.arg(OptionTypeArg), parameters = c.arg(OptionParametersArg).getOrElse(Seq.empty),
          service = c.arg(OptionServiceArg), onlyActive = c.arg(OptionOnlyActiveArg),
          fromTime = c.arg(OptionFromTimeArg), toTime = c.arg(OptionToTimeArg),
          from = c.arg(OptionFromArg), limit = c.arg(OptionLimitArg)) }),
    )
  )

  def Mutations(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      // Login
      Field("login", StringType,
        arguments = AccountArg :: PasswordArg :: Nil,
        resolve = c => { c.ctx.workspace.login(c.arg(AccountArg), c.arg(PasswordArg))
          .map(c.ctx.workspace.encodeAccessToken(_)) }),

      // Accounts management
      Field("addUserAccount", BooleanType,
        arguments = AccountArg :: NameArg :: AccountRoleArg :: PasswordArg :: UserAccountPropertiesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addUserAccount(c.arg(AccountArg), c.arg(NameArg),
          c.arg(AccountRoleArg), c.arg(PasswordArg), c.arg(UserAccountPropertiesArg)).map(_ => true) }),
      Field("addServiceAccount", BooleanType,
        arguments = AccountArg :: NameArg :: AccountRoleArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addServiceAccount(c.arg(AccountArg), c.arg(NameArg),
          c.arg(AccountRoleArg)).map(_ => true) }),
      Field("addConsumerAccount", BooleanType,
        arguments = AccountArg :: NameArg :: ConsumerAccountPropertiesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addConsumerAccount(c.arg(AccountArg), c.arg(NameArg),
          c.arg(ConsumerAccountPropertiesArg)).map(_ => true) }),
      Field("changeUserAccount", BooleanType,
        arguments = OptionAccountArg :: OptionNameArg :: OptionAccountRoleArg ::
          OptionOldPasswordArg :: OptionPasswordArg :: OptionUserAccountPropertiesArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => {
          val account = c.arg(OptionAccountArg).getOrElse(c.ctx.accountInfo.get.account)
          if (c.ctx.accountInfo.get.role != AccountRole.Administrator) {
            if (c.ctx.accessToken.get.account != account) {
              throw AuthorizationException(s"You can change only self account")
            }
            if (!c.arg(OptionOldPasswordArg).isDefined) {
              throw AuthorizationException(s"Old password is not specified")
            }
          }
          c.ctx.workspace.changeUserAccount(account, c.arg(OptionNameArg),
            c.arg(OptionAccountRoleArg),
            c.arg(OptionOldPasswordArg), c.arg(OptionPasswordArg),
            c.arg(OptionUserAccountPropertiesArg))
        }),
      Field("changeServiceAccount", BooleanType,
        arguments = OptionAccountArg :: OptionNameArg :: OptionAccountRoleArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => {
          val account = c.arg(OptionAccountArg).getOrElse(c.ctx.accountInfo.get.account)
          c.ctx.workspace.changeServiceAccount(account, c.arg(OptionNameArg), c.arg(OptionAccountRoleArg))
        }),
      Field("changeConsumerAccount", BooleanType,
        arguments = OptionAccountArg :: OptionNameArg :: OptionConsumerAccountPropertiesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => {
          val account = c.arg(OptionAccountArg).getOrElse(c.ctx.accountInfo.get.account)
          c.ctx.workspace.changeConsumerAccount(account, c.arg(OptionNameArg), c.arg(OptionConsumerAccountPropertiesArg))
        }),
      Field("removeAccount", BooleanType,
        arguments = AccountArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeAccount(c.arg(AccountArg)) }),

      // Builders
      Field("setBuildDeveloperServiceConfig", BooleanType,
        arguments = ServiceArg :: OptionDistributionArg :: EnvironmentArg ::
          RepositoriesArg :: PrivateFilesArg :: MacroValuesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.setBuildDeveloperServiceConfig(
          c.arg(ServiceArg), c.arg(OptionDistributionArg), c.arg(EnvironmentArg),
          c.arg(RepositoriesArg), c.arg(PrivateFilesArg),
          c.arg(MacroValuesArg)).map(_ => true) }),
      Field("removeBuildDeveloperServiceConfig", BooleanType,
        arguments = ServiceArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeBuildDeveloperServiceConfig(c.arg(ServiceArg)).map(_ => true) }),
      Field("setBuildClientServiceConfig", BooleanType,
        arguments = ServiceArg :: OptionDistributionArg :: EnvironmentArg :: RepositoriesArg ::
          PrivateFilesArg :: MacroValuesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.setBuildClientServiceConfig(
          c.arg(ServiceArg), c.arg(OptionDistributionArg), c.arg(EnvironmentArg),
          c.arg(RepositoriesArg), c.arg(PrivateFilesArg),
          c.arg(MacroValuesArg)).map(_ => true) }),
      Field("removeBuildClientServiceConfig", BooleanType,
        arguments = ServiceArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeBuildClientServiceConfig(c.arg(ServiceArg)).map(_ => true) }),

      // Profiles
      Field("addServicesProfile", BooleanType,
        arguments = ProfileArg :: ServicesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addServicesProfile(c.arg(ProfileArg), c.arg(ServicesArg)).map(_ => true) }),
      Field("changeServicesProfile", BooleanType,
        arguments = ProfileArg :: ServicesArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.changeServicesProfile(c.arg(ProfileArg), c.arg(ServicesArg)).map(_ => true) }),
      Field("removeServicesProfile", BooleanType,
        arguments = ProfileArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeServicesProfile(c.arg(ProfileArg)).map(_ => true) }),

      // Developer versions
      Field("buildDeveloperVersion", StringType,
        arguments = ServiceArg :: DeveloperVersionArg :: CommentArg ::
          OptionBuildClientVersionArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.buildDeveloperVersion(
          c.arg(ServiceArg), c.arg(DeveloperVersionArg), c.ctx.accessToken.get.account,
          c.arg(CommentArg), c.arg(OptionBuildClientVersionArg).getOrElse(false))}),
      Field("addDeveloperVersionInfo", BooleanType,
        arguments = DeveloperVersionInfoArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.addDeveloperVersionInfo(c.arg(DeveloperVersionInfoArg)).map(_ => true) }),
      Field("removeDeveloperVersion", BooleanType,
        arguments = ServiceArg :: DeveloperDistributionVersionArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.removeDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperDistributionVersionArg)) }),
      Field("setDeveloperDesiredVersions", BooleanType,
        arguments = DeveloperDesiredVersionDeltasArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.setDeveloperDesiredVersions(
          c.arg(DeveloperDesiredVersionDeltasArg), c.ctx.accessToken.get.account).map(_ => true) }),
      Field("lastCommitComment", StringType,
        arguments = ServiceArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getLastCommitComment(c.arg(ServiceArg)) }),

      // Client versions
      Field("buildClientVersions", StringType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.buildClientVersions(c.arg(DeveloperDesiredVersionsArg),
          c.ctx.accessToken.get.account) }),
      Field("addClientVersionInfo", BooleanType,
        arguments = ClientVersionInfoArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.addClientVersionInfo(c.arg(ClientVersionInfoArg)).map(_ => true) }),
      Field("removeClientVersion", BooleanType,
        arguments = ServiceArg :: ClientDistributionVersionArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.removeClientVersion(c.arg(ServiceArg), c.arg(ClientDistributionVersionArg)) }),
      Field("setClientDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionDeltasArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.setClientDesiredVersions(
          c.arg(ClientDesiredVersionDeltasArg), c.ctx.accessToken.get.account).map(_ => true) }),

      // Distribution providers management
      Field("addProvider", BooleanType,
        arguments = DistributionArg :: UrlArg :: AccessTokenArg :: OptionTestConsumerArg ::
          OptionUploadStateArg :: OptionAutoUpdateArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addProvider(c.arg(DistributionArg), c.arg(UrlArg), c.arg(AccessTokenArg),
          c.arg(OptionTestConsumerArg), c.arg(OptionUploadStateArg), c.arg(OptionAutoUpdateArg)).map(_ => true) }),
      Field("changeProvider", BooleanType,
        arguments = DistributionArg :: UrlArg :: AccessTokenArg :: OptionTestConsumerArg ::
          OptionUploadStateArg :: OptionAutoUpdateArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.changeProvider(c.arg(DistributionArg), c.arg(UrlArg), c.arg(AccessTokenArg),
          c.arg(OptionTestConsumerArg), c.arg(OptionUploadStateArg), c.arg(OptionAutoUpdateArg)).map(_ => true) }),
      Field("removeProvider", BooleanType,
        arguments = DistributionArg :: Nil,
        tags = Authorized(AccountRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeProvider(c.arg(DistributionArg)).map(_ => true) }),

      // Distribution consumers operations
      Field("setProviderTestedVersions", BooleanType,
        arguments = DistributionArg :: DeveloperDesiredVersionsArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.setProviderTestedVersions(c.arg(DistributionArg),
          c.arg(DeveloperDesiredVersionsArg)).map(_ => true) }),
      Field("setTestedVersions", BooleanType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        tags = Authorized(AccountRole.Administrator, AccountRole.Developer, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => { c.ctx.workspace.setTestedVersions(
          if (c.ctx.accountInfo.get.role == AccountRole.DistributionConsumer) c.ctx.accessToken.get.account else c.ctx.workspace.config.distribution,
          if (c.ctx.accountInfo.get.role == AccountRole.DistributionConsumer) c.ctx.accountInfo.get.asInstanceOf[ConsumerAccountInfo].properties.profile else Common.SelfConsumerProfile,
          c.arg(DeveloperDesiredVersionsArg)).map(_ => true) }),

      // State
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionsArg :: Nil,
        tags = Authorized(AccountRole.DistributionConsumer) :: Nil,
        resolve = c => { c.ctx.workspace.setConsumerInstalledDesiredVersions(c.ctx.accessToken.get.account, c.arg(ClientDesiredVersionsArg)).map(_ => true) }),
      Field("setServiceStates", BooleanType,
        arguments = InstanceServiceStatesArg :: Nil,
        tags = Authorized(AccountRole.Updater, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => {
          if (c.ctx.accountInfo.get.role == AccountRole.Updater) {
            c.ctx.workspace.setServiceStates(c.ctx.workspace.config.distribution, c.arg(InstanceServiceStatesArg)).map(_ => true)
          } else {
            c.ctx.workspace.setServiceStates(c.ctx.accessToken.get.account, c.arg(InstanceServiceStatesArg)).map(_ => true)
          }
        }),
      Field("addLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: ProcessArg :: OptionTaskArg :: DirectoryArg :: LogLinesArg :: Nil,
        tags = Authorized(AccountRole.Updater) :: Nil,
        resolve = c => {
          c.ctx.workspace.addLogs(
            c.arg(ServiceArg), c.arg(InstanceArg), c.arg(DirectoryArg), c.arg(ProcessArg), c.arg(OptionTaskArg), c.arg(LogLinesArg)).map(_ => true)
        }),
      Field("addFaultReportInfo", BooleanType,
        arguments = ServiceFaultReportArg :: Nil,
        tags = Authorized(AccountRole.Updater, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => {
          if (c.ctx.accountInfo.get.role == AccountRole.Updater) {
            c.ctx.workspace.addServiceFaultReportInfo(c.ctx.workspace.config.distribution, c.arg(ServiceFaultReportArg)).map(_ => true)
          } else {
            c.ctx.workspace.addServiceFaultReportInfo(c.ctx.accessToken.get.account, c.arg(ServiceFaultReportArg)).map(_ => true)
          }
        }),

      // Run builder remotely
      Field("runBuilder", StringType,
        arguments = AccessTokenArg :: ArgumentsArg :: EnvironmentArg :: ServiceArg :: Nil,
        tags = Authorized(AccountRole.DistributionConsumer) :: Nil,
        resolve = c => { c.ctx.workspace.runBuilderByRemoteDistribution(c.ctx.accessToken.get.account,
          c.arg(AccessTokenArg), c.arg(ArgumentsArg), c.arg(EnvironmentArg), c.arg(ServiceArg)) }),

      // Cancel tasks
      Field("cancelTask", BooleanType,
        arguments = TaskArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.DistributionConsumer) :: Nil,
        resolve = c => { c.ctx.workspace.taskManager.cancel(c.arg(TaskArg)) })
    )
  )

  def Subscriptions(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Subscription",
    fields[GraphqlContext, Unit](
      Field.subs("subscribeLogs", ListType(SequencedServiceLogLineType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: OptionProcessArg ::
          OptionTaskArg :: OptionLevelsArg :: OptionUnitArg :: OptionFindArg ::
          OptionFromArg :: OptionPrefetchArg :: Nil,
        tags = Authorized(AccountRole.Developer, AccountRole.Administrator, AccountRole.DistributionConsumer) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeLogs(
          c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg), c.arg(OptionProcessArg),
          c.arg(OptionTaskArg), c.arg(OptionLevelsArg), c.arg(OptionUnitArg), c.arg(OptionFindArg),
          c.arg(OptionFromArg).map(_.toLong), c.arg(OptionPrefetchArg))),
      Field.subs("testSubscription", StringType,
        tags = Authorized(AccountRole.Developer) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.testSubscription())
    ))

  def SchemaDefinition(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) =
    Schema(query = Queries, mutation = Some(Mutations), subscription = Some(Subscriptions))
}