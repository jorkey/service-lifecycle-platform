package distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UserRole.UserRole
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.DatabaseCollections
import distribution.config.VersionHistoryConfig
import distribution.graphql.GraphqlTypes._
import distribution.graphql.utils.{ClientVersionUtils, ClientsUtils, DeveloperVersionUtils, GetUtils, PutUtils, StateUtils}

import scala.concurrent.ExecutionContext
import sangria.marshalling.sprayJson._
import sangria.schema.{Field, _}

case class GraphqlContext(versionHistoryConfig: VersionHistoryConfig,
                          dir: DistributionDirectory, collections: DatabaseCollections, userInfo: UserInfo)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext,
                         protected val filesLocker: SmartFilesLocker)
    extends ClientsUtils with DeveloperVersionUtils with ClientVersionUtils with StateUtils with GetUtils with PutUtils


object GraphqlSchema {
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  // Arguments

  val ClientArg = Argument("client", StringType)
  val InstanceArg = Argument("instance", StringType)
  val DirectoryArg = Argument("directory", StringType)
  val ServiceArg = Argument("service", StringType)
  val VersionArg = Argument("version", BuildVersionType)
  val BuildInfoArg = Argument("buildInfo", BuildVersionInfoInputType)
  val InstallInfoArg = Argument("installInfo", InstallVersionInfoInputType)
  val DesiredVersionsArg = Argument("versions", ListInputType(DesiredVersionInfoInputType))
  val InstancesStateArg = Argument("state", ListInputType(InstanceServiceStateInputType))
  val LogLinesArg = Argument("logs", ListInputType(LogLineInputType))

  val OptionClientArg = Argument("client", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionVersionArg = Argument("version", OptionInputType(BuildVersionType))
  val OptionLastArg = Argument("last", OptionInputType(IntType))
  val OptionMergedArg = Argument("merged", OptionInputType(BooleanType))

  // Queries

  def CommonQueries = fields[GraphqlContext, Unit](
    Field("userInfo", fieldType = UserInfoType,
      resolve = c => { c.ctx.userInfo }),
  )

  def AdministratorQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("developerVersionsInfo", ListType(DeveloperVersionInfoType),
        arguments = ServiceArg :: OptionClientArg :: OptionVersionArg :: Nil,
        resolve = c => { c.ctx.getDeveloperVersionsInfo(c.arg(ServiceArg), clientName = c.arg(OptionClientArg), version = c.arg(OptionVersionArg)) }),
      Field("developerDesiredVersions", ListType(DesiredVersionType),
        arguments = OptionClientArg :: OptionServicesArg :: OptionMergedArg :: Nil,
        resolve = c => { c.ctx.getDeveloperDesiredVersions(c.arg(OptionClientArg),
          c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet, c.arg(OptionMergedArg).getOrElse(false)) }),

      Field("clientVersionsInfo", ListType(ClientVersionInfoType),
        arguments = ServiceArg :: OptionVersionArg :: Nil,
        resolve = c => { c.ctx.getClientVersionsInfo(c.arg(ServiceArg), version = c.arg(OptionVersionArg)) }),
      Field("clientDesiredVersions", ListType(DesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      Field("clientsInfo", ListType(ClientInfoType),
        resolve = c => c.ctx.getClientsInfo()),
      Field("installedDesiredVersions", ListType(DesiredVersionType),
        arguments = ClientArg :: OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getInstalledDesiredVersions(c.arg(ClientArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("servicesState", ListType(ClientServiceStateType),
        arguments = OptionClientArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        resolve = c => { c.ctx.getServicesState(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
      Field("faultReports", ListType(ClientFaultReportType),
        arguments = OptionClientArg :: OptionServiceArg :: OptionLastArg :: Nil,
        resolve = c => { c.ctx.getClientFaultReports(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) })
  ))

  val ClientQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", ListType(DesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getDeveloperDesiredVersions(Some(c.ctx.userInfo.name), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet, true) })
    )
  )

  val ServiceQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("desiredVersions", ListType(DesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceState", OptionType(InstanceServiceStateType),
        arguments = ServiceArg :: InstanceArg :: DirectoryArg :: Nil,
        resolve = c => { c.ctx.getServiceState(c.arg(ServiceArg), c.arg(InstanceArg), c.arg(DirectoryArg)) })
    )
  )

  // Mutations

  def AdministratorMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("addDeveloperVersionInfo", DeveloperVersionInfoType,
        arguments = ServiceArg :: VersionArg :: BuildInfoArg :: Nil,
        resolve = c => { c.ctx.addDeveloperVersionInfo(c.arg(ServiceArg), c.arg(VersionArg), c.arg(BuildInfoArg)) }),
      Field("removeDeveloperVersion", BooleanType,
        arguments = ServiceArg :: VersionArg :: Nil,
        resolve = c => { c.ctx.removeDeveloperVersion(c.arg(ServiceArg), c.arg(VersionArg)) }),
      Field("addClientVersionInfo", ClientVersionInfoType,
        arguments = ServiceArg :: VersionArg :: BuildInfoArg :: Nil,
        resolve = c => { c.ctx.addClientVersionInfo(c.arg(ServiceArg), c.arg(VersionArg), c.arg(BuildInfoArg), c.arg(InstallInfoArg)) }),
      Field("removeClientVersion", BooleanType,
        arguments = ServiceArg :: VersionArg :: Nil,
        resolve = c => { c.ctx.removeClientVersion(c.arg(ServiceArg), c.arg(VersionArg)) }),
      Field("setDeveloperDesiredVersions", BooleanType,
        arguments = OptionClientArg :: DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setDeveloperDesiredVersions(c.arg(OptionClientArg), c.arg(DesiredVersionsArg)) }),
      Field("setClientDesiredVersions", BooleanType,
        arguments = DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setClientDesiredVersions(c.arg(DesiredVersionsArg)) })
  ))

  val ClientMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("setTestedVersions", BooleanType,
        arguments = DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setTestedVersions(c.ctx.userInfo.name, c.arg(DesiredVersionsArg)) }),
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.addInstalledDesiredVersions(c.ctx.userInfo.name, c.arg(DesiredVersionsArg)) }),
      Field("setServicesState", BooleanType,
        arguments = InstancesStateArg :: Nil,
        resolve = c => { c.ctx.setServicesState(Some(c.ctx.userInfo.name), c.arg(InstancesStateArg)) }))
  )

  val ServiceMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("setServicesState", BooleanType,
        arguments = InstancesStateArg :: Nil,
        resolve = c => { c.ctx.setServicesState(None, c.arg(InstancesStateArg)) }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: DirectoryArg :: LogLinesArg :: Nil,
        resolve = c => { c.ctx.addServiceLogs(None, c.arg(ServiceArg), c.arg(InstanceArg), c.arg(DirectoryArg), c.arg(LogLinesArg)) }))
  )

  val AdministratorSchemaDefinition = Schema(query = AdministratorQueries, mutation = Some(AdministratorMutations))

  val ClientSchemaDefinition = Schema(query = ClientQueries, mutation = Some(ClientMutations))

  val ServiceSchemaDefinition = Schema(query = ServiceQueries, mutation = Some(ServiceMutations))

  def SchemaDefinition(userRole: UserRole): Schema[GraphqlContext, Unit] = {
    userRole match {
      case UserRole.Administrator =>
        AdministratorSchemaDefinition
      case UserRole.Client =>
        ClientSchemaDefinition
      case UserRole.Service =>
        ServiceSchemaDefinition
      case _ =>
        throw new RuntimeException(s"Invalid user role ${userRole} to make graphql schema")
    }
  }
}