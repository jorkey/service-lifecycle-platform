package com.vyulabs.update.distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.{AccessToken, UserRole}
import com.vyulabs.update.distribution.graphql.GraphqlTypes._
import com.vyulabs.update.distribution.graphql.utils._
import com.vyulabs.update.distribution.mongo.DatabaseCollections
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger
import sangria.marshalling.sprayJson._
import sangria.schema.{Field, _}
import sangria.streaming.akkaStreams._

import scala.concurrent.ExecutionContext

case class GraphqlWorkspace(config: DistributionConfig, collections: DatabaseCollections, directory: DistributionDirectory, taskManager: TaskManager)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext)
    extends SourceUtils with DistributionInfoUtils with ServiceProfilesUtils with DistributionConsumersUtils with DistributionProvidersUtils
      with DeveloperVersionUtils with ClientVersionUtils with StateUtils with RunBuilderUtils with UsersUtils

case class GraphqlContext(accessToken: Option[AccessToken], workspace: GraphqlWorkspace)

object GraphqlSchema {
  // Arguments

  val UserArg = Argument("user", StringType)
  val HumanArg = Argument("human", BooleanType)
  val NameArg = Argument("name", StringType)
  val OldPasswordArg = Argument("oldPassword", StringType)
  val PasswordArg = Argument("password", StringType)
  val UserRolesArg = Argument("roles", ListInputType(UserRoleType))
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
  val DeveloperDistributionVersionUniqueArg = Argument("developerVersion", DeveloperDistributionVersionInputType)
  val ClientDistributionVersionUniqueArg = Argument("clientVersion", ClientDistributionVersionInputType)
  val DeveloperVersionInfoArg = Argument("info", DeveloperVersionInfoInputType)
  val ClientVersionInfoArg = Argument("info", ClientVersionInfoInputType)
  val DeveloperDesiredVersionsArg = Argument("versions", ListInputType(DeveloperDesiredVersionInputType))
  val ClientDesiredVersionsArg = Argument("versions", ListInputType(ClientDesiredVersionInputType))
  val DeveloperDesiredVersionDeltasArg = Argument("versions", ListInputType(DeveloperDesiredVersionDeltaInputType))
  val ClientDesiredVersionDeltasArg = Argument("versions", ListInputType(ClientDesiredVersionDeltaInputType))
  val ServiceStatesArg = Argument("states", ListInputType(ServiceStateInputType))
  val InstanceServiceStatesArg = Argument("states", ListInputType(InstanceServiceStateInputType))
  val LogLinesArg = Argument("logs", ListInputType(LogLineInputType))
  val ServiceFaultReportInfoArg = Argument("fault", ServiceFaultReportInputType)
  val ArgumentsArg = Argument("arguments", ListInputType(StringType))
  val SourcesArg = Argument("sources", ListInputType(SourceConfigInputType))
  val UrlArg = Argument("url", UrlType)
  val CommentArg = Argument("comment", StringType)

  val OptionUserArg = Argument("user", OptionInputType(StringType))
  val OptionHumanArg = Argument("human", OptionInputType(BooleanType))
  val OptionNameArg = Argument("name", OptionInputType(StringType))
  val OptionOldPasswordArg = Argument("oldPassword", OptionInputType(StringType))
  val OptionPasswordArg = Argument("password", OptionInputType(StringType))
  val OptionUserRolesArg = Argument("roles", OptionInputType(ListInputType(UserRoleType)))
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
  val OptionLastArg = Argument("last", OptionInputType(IntType))
  val OptionFromArg = Argument("from", OptionInputType(LongType))
  val OptionUploadStateIntervalSecArg = Argument("uploadStateIntervalSec", OptionInputType(IntType))
  val OptionTestConsumerArg = Argument("testConsumer", OptionInputType(StringType))

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
      Field("whoAmI", UserInfoType,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.whoAmI(c.ctx.accessToken.get.user) }),

      // Users
      Field("usersInfo", ListType(UserInfoType),
        arguments = OptionUserArg :: OptionHumanArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getUsersInfo(c.arg(OptionUserArg), c.arg(OptionHumanArg)) }),

      // Sources
      Field("developerServices", ListType(StringType),
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperServices() }),
      Field("serviceSources", ListType(SourceConfigType),
        arguments = ServiceArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceSources(c.arg(ServiceArg)) }),

      // Profiles
      Field("serviceProfiles", ListType(ServicesProfileType),
        arguments = OptionProfileArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceProfiles(c.arg(OptionProfileArg)) }),

      // Developer versions
      Field("developerVersionsInProcess", ListType(DeveloperVersionInProcessInfoType),
        arguments = OptionServiceArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Distribution, UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInProcess(c.arg(OptionServiceArg)) }),
      Field("developerVersionsInfo", ListType(DeveloperVersionInfoType),
        arguments = OptionServiceArg :: OptionDistributionArg :: OptionDeveloperVersionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Distribution, UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInfo(c.arg(OptionServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("developerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Builder, UserRole.Distribution) :: Nil,
        resolve = c => {
          if (c.ctx.accessToken.get.roles.contains(UserRole.Distribution)) {
            c.ctx.workspace.getDeveloperDesiredVersions(c.ctx.accessToken.get.user, c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet)
          } else {
            c.ctx.workspace.getDeveloperDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet)
          }
        }),

      // Client versions
      Field("clientVersionsInfo", ListType(ClientVersionInfoType),
        arguments = ServiceArg :: OptionDistributionArg :: OptionClientVersionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getClientVersionsInfo(c.arg(ServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionClientVersionArg)) }),
      Field("clientDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Builder, UserRole.Updater) :: Nil,
        resolve = c => { c.ctx.workspace.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      // Distribution consumers
      Field("consumersInfo", ListType(ConsumerInfoType),
        arguments = OptionDistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getConsumersInfo(c.arg(OptionDistributionArg)) }),

      // Distribution providers
      Field("providersInfo", ListType(ProviderInfoType),
        arguments = OptionDistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getProvidersInfo(c.arg(OptionDistributionArg)) }),
      Field("providerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getProviderDesiredVersions(c.arg(DistributionArg)) }),

      // State
      Field("installedDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = DistributionArg :: OptionServicesArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getInstalledDesiredVersions(c.arg(DistributionArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceStates", ListType(DistributionServiceStateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Updater) :: Nil,
        resolve = c => { c.ctx.workspace.getServicesState(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
      Field("serviceLogs", ListType(LogLineType),
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceLogs(c.arg(DistributionArg), c.arg(ServiceArg),
          c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("taskLogs", ListType(LogLineType),
        arguments = TaskArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("faultReports", ListType(DistributionFaultReportType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionLastArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionFaultReportsInfo(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) }),
    )
  )

  def Mutations(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      // Login
      Field("login", StringType,
        arguments = UserArg :: PasswordArg :: Nil,
        resolve = c => { c.ctx.workspace.login(c.arg(UserArg), c.arg(PasswordArg))
          .map(c.ctx.workspace.encodeAccessToken(_)) }),

      // Users management
      Field("addUser", BooleanType,
        arguments = UserArg :: HumanArg :: NameArg :: PasswordArg :: UserRolesArg ::
          OptionEmailArg :: OptionNotificationsArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addUser(c.arg(UserArg), c.arg(HumanArg), c.arg(NameArg),
          c.arg(PasswordArg), c.arg(UserRolesArg), c.arg(OptionEmailArg), c.arg(OptionNotificationsArg)).map(_ => true) }),
      Field("removeUser", BooleanType,
        arguments = UserArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeUser(c.arg(UserArg)) }),
      Field("changeUser", BooleanType,
        arguments = UserArg :: OptionNameArg :: OptionOldPasswordArg :: OptionPasswordArg :: OptionUserRolesArg :: OptionEmailArg :: OptionNotificationsArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => {
          val token = c.ctx.accessToken.get
          if (!token.hasRole(UserRole.Administrator)) {
            if (token.user != c.arg(UserArg)) {
              throw AuthorizationException(s"You can change only self account")
            }
            if (!c.arg(OptionOldPasswordArg).isDefined) {
              throw AuthorizationException(s"Old password is not specified")
            }
          }
          c.ctx.workspace.changeUser(c.arg(UserArg), c.arg(OptionNameArg),
            c.arg(OptionOldPasswordArg), c.arg(OptionPasswordArg), c.arg(OptionUserRolesArg), c.arg(OptionEmailArg), c.arg(OptionNotificationsArg))
        }),

      // Sources
      Field("addServiceSources", BooleanType,
        arguments = ServiceArg :: SourcesArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addServiceSources(c.arg(ServiceArg), c.arg(SourcesArg)).map(_ => true) }),
      Field("changeServiceSources", BooleanType,
        arguments = ServiceArg :: SourcesArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.changeSources(c.arg(ServiceArg), c.arg(SourcesArg)).map(_ => true) }),
      Field("removeServiceSources", BooleanType,
        arguments = ServiceArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeServiceSources(c.arg(ServiceArg)).map(_ => true) }),

      // Profiles
      Field("addServicesProfile", BooleanType,
        arguments = ProfileArg :: ServicesArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addServicesProfile(c.arg(ProfileArg), c.arg(ServicesArg)).map(_ => true) }),
      Field("changeServicesProfile", BooleanType,
        arguments = ProfileArg :: ServicesArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.changeServicesProfile(c.arg(ProfileArg), c.arg(ServicesArg)).map(_ => true) }),
      Field("removeServicesProfile", BooleanType,
        arguments = ProfileArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeServicesProfile(c.arg(ProfileArg)).map(_ => true) }),

      // Developer versions
      Field("buildDeveloperVersion", StringType,
        arguments = ServiceArg :: DeveloperVersionArg :: SourcesArg :: CommentArg :: Nil,
        tags = Authorized(UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.buildDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperVersionArg), c.ctx.accessToken.get.user,
          c.arg(SourcesArg), c.arg(CommentArg)) }),
      Field("addDeveloperVersionInfo", BooleanType,
        arguments = DeveloperVersionInfoArg :: Nil,
        tags = Authorized(UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.addDeveloperVersionInfo(c.arg(DeveloperVersionInfoArg)).map(_ => true) }),
      Field("removeDeveloperVersion", BooleanType,
        arguments = ServiceArg :: DeveloperDistributionVersionArg :: Nil,
        tags = Authorized(UserRole.Administrator, UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.removeDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperDistributionVersionArg)) }),
      Field("setDeveloperDesiredVersions", BooleanType,
        arguments = DeveloperDesiredVersionDeltasArg :: Nil,
        tags = Authorized(UserRole.Administrator, UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.setDeveloperDesiredVersions(c.arg(DeveloperDesiredVersionDeltasArg)).map(_ => true) }),

      // Client versions
      Field("buildClientVersion", StringType,
        arguments = ServiceArg :: DeveloperDistributionVersionUniqueArg :: ClientDistributionVersionUniqueArg :: Nil,
        tags = Authorized(UserRole.Administrator, UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.buildClientVersion(c.arg(ServiceArg), c.arg(DeveloperDistributionVersionUniqueArg),
          c.arg(ClientDistributionVersionUniqueArg), c.ctx.accessToken.get.user) }),
      Field("addClientVersionInfo", BooleanType,
        arguments = ClientVersionInfoArg :: Nil,
        tags = Authorized(UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.addClientVersionInfo(c.arg(ClientVersionInfoArg)).map(_ => true) }),
      Field("removeClientVersion", BooleanType,
        arguments = ServiceArg :: ClientDistributionVersionArg :: Nil,
        tags = Authorized(UserRole.Administrator, UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.removeClientVersion(c.arg(ServiceArg), c.arg(ClientDistributionVersionArg)) }),
      Field("setClientDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionDeltasArg :: Nil,
        tags = Authorized(UserRole.Administrator, UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.setClientDesiredVersions(c.arg(ClientDesiredVersionDeltasArg)).map(_ => true) }),

      // Distribution providers management
      Field("addProvider", BooleanType,
        arguments = DistributionArg :: UrlArg :: OptionUploadStateIntervalSecArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addProvider(c.arg(DistributionArg), c.arg(UrlArg), c.arg(OptionUploadStateIntervalSecArg)).map(_ => true) }),
      Field("changeProvider", BooleanType,
        arguments = DistributionArg :: UrlArg :: OptionUploadStateIntervalSecArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.changeProvider(c.arg(DistributionArg), c.arg(UrlArg), c.arg(OptionUploadStateIntervalSecArg)).map(_ => true) }),
      Field("removeProvider", BooleanType,
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeProvider(c.arg(DistributionArg)).map(_ => true) }),

      // Distribution consumers management
      Field("addConsumer", BooleanType,
        arguments = DistributionArg :: ProfileArg :: OptionTestConsumerArg :: Nil,
        tags = Authorized(UserRole.Builder, UserRole.Administrator) :: Nil,
        resolve = c => {
          c.ctx.workspace.addConsumer(c.arg(DistributionArg), c.arg(ProfileArg), c.arg(OptionTestConsumerArg)).map(_ => true)
        }),
      Field("changeConsumer", BooleanType,
        arguments = DistributionArg :: ProfileArg :: OptionTestConsumerArg :: Nil,
        tags = Authorized(UserRole.Builder, UserRole.Administrator) :: Nil,
        resolve = c => {
          c.ctx.workspace.changeConsumer(c.arg(DistributionArg), c.arg(ProfileArg), c.arg(OptionTestConsumerArg)).map(_ => true)
        }),
      Field("removeConsumer", BooleanType,
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeConsumer(c.arg(DistributionArg)).map(_ => true) }),

      // Distribution consumers operations
      Field("installProviderVersion", StringType,
        arguments = DistributionArg:: ServiceArg :: DeveloperDistributionVersionArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.installProviderVersion(c.arg(DistributionArg), c.arg(ServiceArg), c.arg(DeveloperDistributionVersionArg)) }),
      Field("setTestedVersions", BooleanType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        tags = Authorized(UserRole.Distribution) :: Nil,
        resolve = c => { c.ctx.workspace.setTestedVersions(c.ctx.accessToken.get.user, c.arg(DeveloperDesiredVersionsArg)).map(_ => true) }),

      // State
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionsArg :: Nil,
        tags = Authorized(UserRole.Distribution) :: Nil,
        resolve = c => { c.ctx.workspace.setInstalledDesiredVersions(c.ctx.accessToken.get.user, c.arg(ClientDesiredVersionsArg)).map(_ => true) }),
      Field("setServiceStates", BooleanType,
        arguments = InstanceServiceStatesArg :: Nil,
        tags = Authorized(UserRole.Updater, UserRole.Distribution) :: Nil,
        resolve = c => {
          if (c.ctx.accessToken.get.roles.contains(UserRole.Updater)) {
            c.ctx.workspace.setServiceStates(c.ctx.workspace.config.distribution, c.arg(InstanceServiceStatesArg)).map(_ => true)
          } else {
            c.ctx.workspace.setServiceStates(c.ctx.accessToken.get.user, c.arg(InstanceServiceStatesArg)).map(_ => true)
          }
        }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: ProcessArg :: OptionTaskArg :: DirectoryArg :: LogLinesArg :: Nil,
        tags = Authorized(UserRole.Updater, UserRole.Distribution) :: Nil,
        resolve = c => {
          if (c.ctx.accessToken.get.roles.contains(UserRole.Updater)) {
            c.ctx.workspace.addServiceLogs(c.ctx.workspace.config.distribution,
              c.arg(ServiceArg), c.arg(OptionTaskArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(LogLinesArg)).map(_ => true)
          } else {
            c.ctx.workspace.addServiceLogs(c.ctx.accessToken.get.user,
              c.arg(ServiceArg), c.arg(OptionTaskArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(LogLinesArg)).map(_ => true)
          }
        }),
      Field("addFaultReportInfo", BooleanType,
        arguments = ServiceFaultReportInfoArg :: Nil,
        tags = Authorized(UserRole.Updater, UserRole.Distribution) :: Nil,
        resolve = c => {
          if (c.ctx.accessToken.get.roles.contains(UserRole.Updater)) {
            c.ctx.workspace.addServiceFaultReportInfo(c.ctx.workspace.config.distribution, c.arg(ServiceFaultReportInfoArg)).map(_ => true)
          } else {
            c.ctx.workspace.addServiceFaultReportInfo(c.ctx.accessToken.get.user, c.arg(ServiceFaultReportInfoArg)).map(_ => true)
          }
        }),

      // Run builder remotely
      Field("runBuilder", StringType,
        arguments = ArgumentsArg :: Nil,
        tags = Authorized(UserRole.Distribution) :: Nil,
        resolve = c => { c.ctx.workspace.runLocalBuilderByRemoteDistribution(c.arg(ArgumentsArg)) }),

      // Cancel tasks
      Field("cancelTask", BooleanType,
        arguments = TaskArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Distribution) :: Nil,
        resolve = c => { c.ctx.workspace.taskManager.cancel(c.arg(TaskArg)) })
    )
  )

  def Subscriptions(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Subscription",
    fields[GraphqlContext, Unit](
      Field.subs("subscribeServiceLogs", SequencedLogLineType,
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeServiceLogs(
          c.arg(DistributionArg), c.arg(ServiceArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg))),
      Field.subs("subscribeTaskLogs", SequencedLogLineType,
        arguments = TaskArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Distribution) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg))),
      Field.subs("testSubscription", StringType,
        tags = Authorized(UserRole.Developer) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.testSubscription())
    ))

  def SchemaDefinition(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) =
    Schema(query = Queries, mutation = Some(Mutations), subscription = Some(Subscriptions))
}