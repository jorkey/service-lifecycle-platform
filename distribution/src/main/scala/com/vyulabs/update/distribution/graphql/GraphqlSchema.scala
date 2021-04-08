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
    extends DistributionInfoUtils with DistributionConsumerProfilesUtils with DistributionConsumersUtils with DistributionProvidersUtils
      with DeveloperVersionUtils with ClientVersionUtils with StateUtils with RunBuilderUtils with UsersUtils

case class GraphqlContext(accessToken: Option[AccessToken], workspace: GraphqlWorkspace)

object GraphqlSchema {
  // Arguments

  val UserArg = Argument("user", StringType)
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
  val ConsumerProfileArg = Argument("profile", StringType)
  val UrlArg = Argument("url", UrlType)

  val OptionUserArg = Argument("user", OptionInputType(StringType))
  val OptionOldPasswordArg = Argument("oldPassword", OptionInputType(StringType))
  val OptionPasswordArg = Argument("password", OptionInputType(StringType))
  val OptionUserRolesArg = Argument("roles", OptionInputType(ListInputType(UserRoleType)))
  val OptionHumanInfoArg = Argument("human", OptionInputType(HumanInfoInputType))
  val OptionTaskArg = Argument("task", OptionInputType(StringType))
  val OptionDistributionArg = Argument("distribution", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionProcessArg = Argument("process", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionDeveloperVersionArg = Argument("version", OptionInputType(DeveloperVersionInputType))
  val OptionClientVersionArg = Argument("version", OptionInputType(ClientVersionInputType))
  val OptionMergedArg = Argument("merged", OptionInputType(BooleanType))
  val OptionCommentArg = Argument("comment", OptionInputType(StringType))
  val OptionBranchesArg = Argument("branches", OptionInputType(ListInputType(StringType)))
  val OptionLastArg = Argument("last", OptionInputType(IntType))
  val OptionFromArg = Argument("from", OptionInputType(LongType))
  val OptionConsumerProfileArg = Argument("profile", OptionInputType(StringType))
  val OptionUploadStateIntervalArg = Argument("uploadStateInterval", OptionInputType(FiniteDurationType))
  val OptionTestDistributionMatchArg = Argument("testDistributionMatch", OptionInputType(StringType))

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
        arguments = OptionUserArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getUsersInfo(c.arg(OptionUserArg)) }),

      // Developer versions
      Field("developerVersionsInfo", ListType(DeveloperVersionInfoType),
        arguments = ServiceArg :: OptionDistributionArg :: OptionDeveloperVersionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Distribution, UserRole.Builder) :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInfo(c.arg(ServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionDeveloperVersionArg)) }),
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
      Field("distributionConsumersInfo", ListType(DistributionConsumerInfoType),
        arguments = OptionDistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionConsumersInfo(c.arg(OptionDistributionArg)) }),
      Field("distributionConsumerProfiles", ListType(DistributionConsumerProfileType),
        arguments = OptionConsumerProfileArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionConsumerProfiles(c.arg(OptionConsumerProfileArg)) }),

      // Distribution providers
      Field("distributionProvidersInfo", ListType(DistributionProviderInfoType),
        arguments = OptionDistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionProvidersInfo(c.arg(OptionDistributionArg)) }),
      Field("distributionProviderDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionProviderDesiredVersions(c.arg(DistributionArg)) }),

      // State
      Field("installedDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = DistributionArg :: OptionServicesArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getInstalledDesiredVersions(c.arg(DistributionArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceStates", ListType(DistributionServiceStateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator, UserRole.Updater) :: Nil,
        resolve = c => { c.ctx.workspace.getServicesState(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
      Field("serviceLogs", ListType(ServiceLogLineType),
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getServiceLogs(c.arg(DistributionArg), c.arg(ServiceArg),
          c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("taskLogs", ListType(ServiceLogLineType),
        arguments = TaskArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.getTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("faultReportsInfo", ListType(DistributionFaultReportType),
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
        arguments = UserArg :: PasswordArg :: UserRolesArg :: OptionHumanInfoArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addUser(c.arg(UserArg),
          c.arg(PasswordArg), c.arg(UserRolesArg), c.arg(OptionHumanInfoArg)).map(_ => true) }),
      Field("removeUser", BooleanType,
        arguments = UserArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeUser(c.arg(UserArg)) }),
      Field("changeUser", BooleanType,
        arguments = UserArg :: OptionOldPasswordArg :: OptionPasswordArg :: OptionUserRolesArg :: OptionHumanInfoArg :: Nil,
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
          c.ctx.workspace.changeUser(c.arg(UserArg), c.arg(OptionOldPasswordArg), c.arg(OptionPasswordArg), c.arg(OptionUserRolesArg), c.arg(OptionHumanInfoArg))
        }),

      // Developer versions
      Field("buildDeveloperVersion", StringType,
        arguments = ServiceArg :: DeveloperVersionArg :: OptionBranchesArg :: OptionCommentArg :: Nil,
        tags = Authorized(UserRole.Developer) :: Nil,
        resolve = c => { c.ctx.workspace.buildDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperVersionArg), c.ctx.accessToken.get.user,
          c.arg(OptionBranchesArg).getOrElse(Seq.empty), c.arg(OptionCommentArg)) }),
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
      Field("addDistributionProvider", BooleanType,
        arguments = DistributionArg :: UrlArg :: OptionUploadStateIntervalArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addDistributionProvider(c.arg(DistributionArg), c.arg(UrlArg), c.arg(OptionUploadStateIntervalArg)).map(_ => true) }),
      Field("removeDistributionProvider", BooleanType,
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeDistributionProvider(c.arg(DistributionArg)).map(_ => true) }),

      // Distribution consumers management
      Field("addDistributionConsumerProfile", BooleanType,
        arguments = ConsumerProfileArg :: ServicesArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.addDistributionConsumerProfile(c.arg(ConsumerProfileArg), c.arg(ServicesArg)).map(_ => true) }),
      Field("removeDistributionConsumerProfile", BooleanType,
        arguments = ConsumerProfileArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeDistributionConsumerProfile(c.arg(ConsumerProfileArg)).map(_ => true) }),
      Field("addDistributionConsumer", BooleanType,
        arguments = DistributionArg :: ConsumerProfileArg :: OptionTestDistributionMatchArg :: Nil,
        tags = Authorized(UserRole.Builder, UserRole.Administrator) :: Nil,
        resolve = c => {
          c.ctx.workspace.addDistributionConsumer(c.arg(DistributionArg), c.arg(ConsumerProfileArg), c.arg(OptionTestDistributionMatchArg)).map(_ => true)
        }),
      Field("removeDistributionConsumer", BooleanType,
        arguments = DistributionArg :: Nil,
        tags = Authorized(UserRole.Administrator) :: Nil,
        resolve = c => { c.ctx.workspace.removeDistributionConsumer(c.arg(DistributionArg)).map(_ => true) }),

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
      Field.subs("subscribeServiceLogs", SequencedServiceLogLineType,
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        tags = Authorized(UserRole.Developer, UserRole.Administrator) :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeServiceLogs(
          c.arg(DistributionArg), c.arg(ServiceArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg))),
      Field.subs("subscribeTaskLogs", SequencedServiceLogLineType,
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