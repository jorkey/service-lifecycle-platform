package com.vyulabs.update.distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info.UserRole.UserRole
import com.vyulabs.update.common.info.{UserInfo, UserRole}
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
    extends UsersUtils with DistributionConsumersUtils with DistributionProvidersUtils with DeveloperVersionUtils with ClientVersionUtils with StateUtils with RunBuilderUtils

case class GraphqlContext(userInfo: UserInfo, workspace: GraphqlWorkspace)

object GraphqlSchema {
  // Arguments

  val UserArg = Argument("user", StringType)
  val OldPasswordArg = Argument("oldPassword", StringType)
  val PasswordArg = Argument("password", StringType)
  val UserRoleArg = Argument("role", UserRoleType)
  val DistributionArg = Argument("distribution", StringType)
  val ProviderDistributionArg = Argument("provider", StringType)
  val InstanceArg = Argument("instance", StringType)
  val ProcessArg = Argument("process", StringType)
  val TaskArg = Argument("task", StringType)
  val DirectoryArg = Argument("directory", StringType)
  val ServiceArg = Argument("service", StringType)
  val DeveloperVersionArg = Argument("version", DeveloperVersionType)
  val ClientVersionArg = Argument("version", ClientVersionType)
  val DeveloperDistributionVersionArg = Argument("version", DeveloperDistributionVersionType)
  val ClientDistributionVersionArg = Argument("version", ClientDistributionVersionType)
  val DeveloperDistributionVersionUniqueArg = Argument("developerVersion", DeveloperDistributionVersionType)
  val ClientDistributionVersionUniqueArg = Argument("clientVersion", ClientDistributionVersionType)
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

  val OptionUserArg = Argument("user", OptionInputType(StringType))
  val OptionTaskArg = Argument("task", OptionInputType(StringType))
  val OptionDistributionArg = Argument("distribution", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionProcessArg = Argument("process", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionDeveloperVersionArg = Argument("version", OptionInputType(DeveloperVersionType))
  val OptionClientVersionArg = Argument("version", OptionInputType(ClientVersionType))
  val OptionMergedArg = Argument("merged", OptionInputType(BooleanType))
  val OptionCommentArg = Argument("comment", OptionInputType(StringType))
  val OptionBranchesArg = Argument("branches", OptionInputType(ListInputType(StringType)))
  val OptionLastArg = Argument("last", OptionInputType(IntType))
  val OptionFromArg = Argument("from", OptionInputType(LongType))

  // Queries

  def CommonQueries(implicit log: Logger) = fields[GraphqlContext, Unit](
    Field("userInfo", fieldType = UserInfoType,
      resolve = c => { c.ctx.userInfo }),
  )

  def AdministratorQueries(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("usersInfo", ListType(UserInfoType),
        arguments = OptionUserArg :: Nil,
        resolve = c => { c.ctx.workspace.getUsersInfo(c.arg(OptionUserArg)) }),

      Field("developerVersionsInfo", ListType(DeveloperVersionInfoType),
        arguments = ServiceArg :: OptionDistributionArg :: OptionDeveloperVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInfo(c.arg(ServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("developerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      Field("clientVersionsInfo", ListType(ClientVersionInfoType),
        arguments = ServiceArg :: OptionDistributionArg :: OptionClientVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.getClientVersionsInfo(c.arg(ServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionClientVersionArg)) }),
      Field("clientDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.workspace.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      Field("distributionClientsInfo", ListType(ClientInfoType),
        resolve = c => c.ctx.workspace.getDistributionConsumersInfo()),
      Field("installedDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = DistributionArg :: OptionServicesArg :: Nil,
        resolve = c => { c.ctx.workspace.getInstalledDesiredVersions(c.arg(DistributionArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceStates", ListType(ClientServiceStateType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        resolve = c => { c.ctx.workspace.getServicesState(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
      Field("serviceLogs", ListType(ServiceLogLineType),
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        resolve = c => { c.ctx.workspace.getServiceLogs(c.arg(DistributionArg), c.arg(ServiceArg),
          c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("taskLogs", ListType(ServiceLogLineType),
        arguments = TaskArg :: OptionFromArg :: Nil,
        resolve = c => { c.ctx.workspace.getTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg)).map(_.map(_.document)) }),
      Field("faultReportsInfo", ListType(DistributionFaultReportType),
        arguments = OptionDistributionArg :: OptionServiceArg :: OptionLastArg :: Nil,
        resolve = c => { c.ctx.workspace.getDistributionFaultReportsInfo(c.arg(OptionDistributionArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) }),

      Field("providerDeveloperDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = ProviderDistributionArg :: Nil,
        resolve = c => { c.ctx.workspace.getProviderDeveloperDesiredVersions(c.arg(ProviderDistributionArg)) }),
    )
  )

  def DistributionQueries(implicit log: Logger) = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("distributionClientConfig", ClientConfigInfoType,
        resolve = c => { c.ctx.workspace.getDistributionConsumerConfig(c.ctx.userInfo.name) }),
      Field("versionsInfo", ListType(DeveloperVersionInfoType),
        arguments = ServiceArg :: OptionDistributionArg :: OptionDeveloperVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperVersionsInfo(c.arg(ServiceArg), c.arg(OptionDistributionArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("developerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.workspace.getDeveloperDesiredVersions(c.ctx.userInfo.name, c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) })
    )
  )

  def ServiceQueries(implicit log: Logger) = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("clientDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.workspace.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceStates", ListType(InstanceServiceStateType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        resolve = c => { c.ctx.workspace.getInstanceServicesState(Some(c.ctx.workspace.config.distributionName),
          c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) })
    )
  )

  // Mutations

  def CommonMutations(implicit log: Logger) = fields[GraphqlContext, Unit](
    Field("changePassword", fieldType = BooleanType,
      arguments = OldPasswordArg :: PasswordArg :: Nil,
      resolve = c => { c.ctx.workspace.changeUserPassword(c.ctx.userInfo.name, c.arg(OldPasswordArg), c.arg(PasswordArg)) }),
  )

  def AdministratorMutations(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Mutation",
    CommonMutations ++ fields[GraphqlContext, Unit](
      Field("addUser", BooleanType,
        arguments = UserArg :: UserRoleArg :: PasswordArg :: Nil,
        resolve = c => { c.ctx.workspace.addUser(c.arg(UserArg), c.arg(UserRoleArg), c.arg(PasswordArg)).map(_ => true) }),
      Field("removeUser", BooleanType,
        arguments = UserArg :: Nil,
        resolve = c => { c.ctx.workspace.removeUser(c.arg(UserArg)) }),
      Field("changeUserPassword", BooleanType,
        arguments = UserArg :: PasswordArg :: Nil,
        resolve = c => { c.ctx.workspace.changeUserPassword(c.arg(UserArg), c.arg(PasswordArg)) }),

      Field("buildDeveloperVersion", StringType,
        arguments = ServiceArg :: DeveloperVersionArg :: OptionBranchesArg :: OptionCommentArg :: Nil,
        resolve = c => { c.ctx.workspace.buildDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperVersionArg), c.ctx.userInfo.name,
          c.arg(OptionBranchesArg).getOrElse(Seq.empty), c.arg(OptionCommentArg)) }),
      Field("addDeveloperVersionInfo", BooleanType,
        arguments = DeveloperVersionInfoArg :: Nil,
        resolve = c => { c.ctx.workspace.addDeveloperVersionInfo(c.arg(DeveloperVersionInfoArg)).map(_ => true) }),
      Field("removeDeveloperVersion", BooleanType,
        arguments = ServiceArg :: DeveloperDistributionVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.removeDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperDistributionVersionArg)) }),
      Field("setDeveloperDesiredVersions", BooleanType,
        arguments = DeveloperDesiredVersionDeltasArg :: Nil,
        resolve = c => { c.ctx.workspace.setDeveloperDesiredVersions(c.arg(DeveloperDesiredVersionDeltasArg)).map(_ => true) }),

      Field("buildClientVersion", StringType,
        arguments = ServiceArg :: DeveloperDistributionVersionUniqueArg :: ClientDistributionVersionUniqueArg :: Nil,
        resolve = c => { c.ctx.workspace.buildClientVersion(c.arg(ServiceArg), c.arg(DeveloperDistributionVersionUniqueArg),
          c.arg(ClientDistributionVersionUniqueArg), c.ctx.userInfo.name) }),
      Field("addClientVersionInfo", BooleanType,
        arguments = ClientVersionInfoArg :: Nil,
        resolve = c => { c.ctx.workspace.addClientVersionInfo(c.arg(ClientVersionInfoArg)).map(_ => true) }),
      Field("removeClientVersion", BooleanType,
        arguments = ServiceArg :: ClientDistributionVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.removeClientVersion(c.arg(ServiceArg), c.arg(ClientDistributionVersionArg)) }),
      Field("setClientDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionDeltasArg :: Nil,
        resolve = c => { c.ctx.workspace.setClientDesiredVersions(c.arg(ClientDesiredVersionDeltasArg)).map(_ => true) }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: ProcessArg :: OptionTaskArg :: DirectoryArg :: LogLinesArg :: Nil,
        resolve = c => { c.ctx.workspace.addServiceLogs(c.ctx.workspace.config.distributionName,
          c.arg(ServiceArg), c.arg(OptionTaskArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(LogLinesArg)).map(_ => true) }),

      Field("installProviderDeveloperVersion", StringType,
        arguments = ProviderDistributionArg:: ServiceArg :: DeveloperDistributionVersionArg :: Nil,
        resolve = c => { c.ctx.workspace.installProviderDeveloperVersion(c.arg(ProviderDistributionArg), c.arg(ServiceArg), c.arg(DeveloperDistributionVersionArg)) }),

      Field("cancelTask", BooleanType,
        arguments = TaskArg :: Nil,
        resolve = c => { c.ctx.workspace.taskManager.cancel(c.arg(TaskArg)) })
    )
  )

  def DistributionMutations(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Mutation",
    CommonMutations ++ fields[GraphqlContext, Unit](
      Field("setTestedVersions", BooleanType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.workspace.setTestedVersions(c.ctx.userInfo.name, c.arg(DeveloperDesiredVersionsArg)).map(_ => true) }),
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.workspace.setInstalledDesiredVersions(c.ctx.userInfo.name, c.arg(ClientDesiredVersionsArg)).map(_ => true) }),
      Field("setServiceStates", BooleanType,
        arguments = InstanceServiceStatesArg :: Nil,
        resolve = c => { c.ctx.workspace.setServiceStates(c.ctx.userInfo.name, c.arg(InstanceServiceStatesArg)).map(_ => true) }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: ProcessArg :: OptionTaskArg :: DirectoryArg :: LogLinesArg :: Nil,
        resolve = c => { c.ctx.workspace.addServiceLogs(c.ctx.userInfo.name,
          c.arg(ServiceArg), c.arg(OptionTaskArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(LogLinesArg)).map(_ => true) }),
      Field("addFaultReportInfo", BooleanType,
        arguments = ServiceFaultReportInfoArg :: Nil,
        resolve = c => { c.ctx.workspace.addServiceFaultReportInfo(c.ctx.userInfo.name, c.arg(ServiceFaultReportInfoArg)).map(_ => true) }),
      Field("runBuilder", StringType,
        arguments = ArgumentsArg :: Nil,
        resolve = c => { c.ctx.workspace.runLocalBuilderByRemoteDistribution(c.arg(ArgumentsArg)) }),
      Field("cancelTask", BooleanType,
        arguments = TaskArg :: Nil,
        resolve = c => { c.ctx.workspace.taskManager.cancel(c.arg(TaskArg)) }))
  )

  def ServiceMutations(implicit executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Mutation",
    CommonMutations ++ fields[GraphqlContext, Unit](
      Field("setServiceStates", BooleanType,
        arguments = InstanceServiceStatesArg :: Nil,
        resolve = c => { c.ctx.workspace.setServiceStates(c.ctx.workspace.config.distributionName, c.arg(InstanceServiceStatesArg)).map(_ => true) }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: ProcessArg :: OptionTaskArg :: DirectoryArg :: LogLinesArg :: Nil,
        resolve = c => { c.ctx.workspace.addServiceLogs(c.ctx.workspace.config.distributionName,
          c.arg(ServiceArg), c.arg(OptionTaskArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(LogLinesArg)).map(_ => true) }),
      Field("addFaultReportInfo", BooleanType,
        arguments = ServiceFaultReportInfoArg :: Nil,
        resolve = c => { c.ctx.workspace.addServiceFaultReportInfo(c.ctx.workspace.config.distributionName, c.arg(ServiceFaultReportInfoArg)).map(_ => true) })
    )
  )

  // Subscriptions

  def AdministratorSubscriptions(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Subscription",
    fields[GraphqlContext, Unit](
      Field.subs("subscribeServiceLogs", SequencedServiceLogLineType,
        arguments = DistributionArg :: ServiceArg :: InstanceArg :: ProcessArg :: DirectoryArg :: OptionFromArg :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeServiceLogs(
          c.arg(DistributionArg), c.arg(ServiceArg), c.arg(InstanceArg), c.arg(ProcessArg), c.arg(DirectoryArg), c.arg(OptionFromArg))),
      Field.subs("subscribeTaskLogs", SequencedServiceLogLineType,
        arguments = TaskArg :: OptionFromArg :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg))),
      Field.subs("testSubscription", StringType,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.testSubscription())
    ))

  def DistributionSubscriptions(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) = ObjectType(
    "Subscription",
    fields[GraphqlContext, Unit](
      Field.subs("subscribeTaskLogs", SequencedServiceLogLineType,
        arguments = TaskArg :: OptionFromArg :: Nil,
        resolve = (c: Context[GraphqlContext, Unit]) => c.ctx.workspace.subscribeTaskLogs(c.arg(TaskArg), c.arg(OptionFromArg)))
    ))

  def AdministratorSchemaDefinition(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) =
    Schema(query = AdministratorQueries, mutation = Some(AdministratorMutations), subscription = Some(AdministratorSubscriptions))

  def DistributionSchemaDefinition(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger) =
    Schema(query = DistributionQueries, mutation = Some(DistributionMutations), subscription = Some(DistributionSubscriptions))

  def ServiceSchemaDefinition(implicit executionContext: ExecutionContext, log: Logger) =
    Schema(query = ServiceQueries, mutation = Some(ServiceMutations))

  def SchemaDefinition(userRole: UserRole)(implicit materializer: Materializer, executionContext: ExecutionContext, log: Logger): Schema[GraphqlContext, Unit] = {
    userRole match {
      case UserRole.Administrator =>
        AdministratorSchemaDefinition
      case UserRole.Distribution =>
        DistributionSchemaDefinition
      case UserRole.Service =>
        ServiceSchemaDefinition
      case _ =>
        throw new RuntimeException(s"Invalid user role ${userRole} to make graphql schema")
    }
  }
}