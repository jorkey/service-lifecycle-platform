package distribution.graphql

import java.io.File

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
import distribution.utils.{ClientsUtils, CommonUtils, GetUtils, PutUtils, StateUtils, VersionUtils}

import scala.concurrent.ExecutionContext
import sangria.marshalling.sprayJson._
import sangria.schema._

case class GraphqlContext(versionHistoryConfig: VersionHistoryConfig,
                          dir: DistributionDirectory, collections: DatabaseCollections, userInfo: UserInfo)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext,
                         protected val filesLocker: SmartFilesLocker) extends ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils

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

  val OptionClientArg = Argument("client", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionVersionArg = Argument("version", OptionInputType(BuildVersionType))
  val OptionLastArg = Argument("last", OptionInputType(IntType))
  val OptionMergedArg = Argument("merged", OptionInputType(BooleanType))

  // Queries

  def CommonQueries[T <: GraphqlContext] = fields[T, Unit](
    Field("userInfo", fieldType = UserInfoType,
      resolve = c => { c.ctx.userInfo }),
  )

  def AdministratorQueries[T <: GraphqlContext] = CommonQueries[T] ++ fields[T, Unit](
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
      arguments = OptionClientArg :: OptionServicesArg :: OptionMergedArg :: Nil,
      resolve = c => { c.ctx.getDeveloperDesiredVersions(c.arg(OptionClientArg),
        c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet, c.arg(OptionMergedArg).getOrElse(false)) }),

    Field("clientsInfo", ListType(ClientInfoType),
      resolve = c => c.ctx.getClientsInfo()),
    Field("servicesState", ListType(ClientServiceStateType),
      arguments = OptionClientArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
      resolve = c => { c.ctx.getServicesState(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
    Field("faultReports", ListType(ClientFaultReportType),
      arguments = OptionClientArg :: OptionServiceArg :: OptionLastArg :: Nil,
      resolve = c => { c.ctx.getClientFaultReports(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) })
  )

  val ClientQueries = ObjectType(
    "Query",
    CommonQueries[GraphqlContext] ++ fields[GraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", ListType(DesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getDeveloperDesiredVersions(Some(c.ctx.userInfo.name),
          c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet, true) }),
    )
  )

  // Mutations

  def AdministratorMutations[T <: GraphqlContext] = fields[T, Unit](
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
    Field("clientDesiredVersions", BooleanType,
      arguments = OptionClientArg :: DesiredVersionsArg :: Nil,
      resolve = c => { c.ctx.setClientDesiredVersions(c.arg(OptionClientArg), c.arg(DesiredVersionsArg)) })
  )

  val ClientMutations = ObjectType(
    "Mutation",
    AdministratorMutations[GraphqlContext] ++ fields[GraphqlContext, Unit](
      Field("setTestedVersions", BooleanType,
        arguments = DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setTestedVersions(c.ctx.userInfo.name, c.arg(DesiredVersionsArg)) }),
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = DesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setClientDesiredVersions(Some(c.ctx.userInfo.name), c.arg(DesiredVersionsArg)) }),
      Field("setServicesState", BooleanType,
        arguments = InstancesStateArg :: Nil,
        resolve = c => { c.ctx.setServicesState(c.ctx.userInfo.name, c.arg(InstancesStateArg)) }))
      // TODO serviceFault
  )

  val ServiceMutations = ObjectType(
    "Mutation",
    AdministratorMutations[GraphqlContext] ++ fields[GraphqlContext, Unit](
      // setServicesState
      // addServiceLogs
      //
    )
  )

  val ClientSchemaDefinition = Schema(query = ClientQueries, mutation = Some(ClientMutations))

  val AdministratorSchemaDefinition = Schema(query = ObjectType("Query", AdministratorQueries[GraphqlContext]),
    mutation = Some(ObjectType("Mutation", AdministratorMutations[GraphqlContext])))

  def SchemaDefinition(userRole: UserRole): Schema[GraphqlContext, Unit] = {
    userRole match {
      case UserRole.Administrator =>
        AdministratorSchemaDefinition
      case UserRole.Client =>
        ClientSchemaDefinition
      case _ =>
        throw new RuntimeException(s"Invalid user role ${userRole} to make graphql schema")
    }
  }
}