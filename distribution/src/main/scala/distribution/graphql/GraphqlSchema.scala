package distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.DistributionName
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.users.UserRole.UserRole
import com.vyulabs.update.users.{UserInfo, UserRole}
import distribution.config.VersionHistoryConfig
import distribution.graphql.GraphqlTypes._
import distribution.graphql.utils.{ClientVersionUtils, ClientsUtils, DeveloperVersionUtils, StateUtils}
import distribution.mongo.DatabaseCollections

import scala.concurrent.ExecutionContext
import sangria.marshalling.sprayJson._
import sangria.schema.{Field, _}

case class GraphqlContext(distributionName: DistributionName,
                          versionHistoryConfig: VersionHistoryConfig,
                          dir: DistributionDirectory, collections: DatabaseCollections, userInfo: UserInfo)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext)
    extends ClientsUtils with DeveloperVersionUtils with ClientVersionUtils with StateUtils


object GraphqlSchema {
  private implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  // Arguments

  val ClientArg = Argument("client", StringType)
  val InstanceArg = Argument("instance", StringType)
  val DirectoryArg = Argument("directory", StringType)
  val ServiceArg = Argument("service", StringType)
  val DeveloperVersionArg = Argument("version", DeveloperDistributionVersionType)
  val ClientVersionArg = Argument("version", ClientDistributionVersionType)
  val DeveloperVersionInfoArg = Argument("info", DeveloperVersionInfoInputType)
  val InstalledVersionInfoArg = Argument("info", InstalledVersionInfoInputType)
  val DeveloperDesiredVersionsArg = Argument("versions", ListInputType(DeveloperDesiredVersionInfoInputType))
  val ClientDesiredVersionsArg = Argument("versions", ListInputType(ClientDesiredVersionInfoInputType))
  val InstancesStateArg = Argument("state", ListInputType(InstanceServiceStateInputType))
  val LogLinesArg = Argument("logs", ListInputType(LogLineInputType))

  val OptionClientArg = Argument("client", OptionInputType(StringType))
  val OptionInstanceArg = Argument("instance", OptionInputType(StringType))
  val OptionDirectoryArg = Argument("directory", OptionInputType(StringType))
  val OptionServiceArg = Argument("service", OptionInputType(StringType))
  val OptionServicesArg = Argument("services", OptionInputType(ListInputType(StringType)))
  val OptionDeveloperVersionArg = Argument("version", OptionInputType(DeveloperDistributionVersionType))
  val OptionClientVersionArg = Argument("version", OptionInputType(ClientDistributionVersionType))
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
        arguments = ServiceArg :: OptionDeveloperVersionArg :: Nil,
        resolve = c => { c.ctx.getDeveloperVersionsInfo(c.arg(ServiceArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("developerDesiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getDeveloperDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      Field("clientVersionsInfo", ListType(ClientVersionInfoType),
        arguments = ServiceArg :: OptionDeveloperVersionArg :: Nil,
        resolve = c => { c.ctx.getClientVersionsInfo(c.arg(ServiceArg), version = c.arg(OptionDeveloperVersionArg)) }),
      Field("clientDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),

      Field("distributionClientsInfo", ListType(ClientInfoType),
        resolve = c => c.ctx.getClientsInfo()),
      Field("installedDesiredVersions", ListType(ClientDesiredVersionType),
        arguments = ClientArg :: OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getInstalledDesiredVersions(c.arg(ClientArg), c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("servicesState", ListType(ClientServiceStateType),
        arguments = OptionClientArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        resolve = c => { c.ctx.getServicesState(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
      Field("faultReports", ListType(ClientFaultReportType),
        arguments = OptionClientArg :: OptionServiceArg :: OptionLastArg :: Nil,
        resolve = c => { c.ctx.getClientFaultReports(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) })
  ))

  val DistributionQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", ListType(DeveloperDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getDeveloperDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) })
    )
  )

  val ServiceQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[GraphqlContext, Unit](
      Field("desiredVersions", ListType(ClientDesiredVersionType),
        arguments = OptionServicesArg :: Nil,
        resolve = c => { c.ctx.getClientDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) }),
      Field("serviceState", ListType(InstanceServiceStateType),
        arguments = OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
        resolve = c => { c.ctx.getServicesState(Some(c.ctx.distributionName), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg))
          .map(_.map(_.instance)) })
    )
  )

  // Mutations

  def AdministratorMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("addDeveloperVersionInfo", BooleanType,
        arguments = DeveloperVersionInfoArg :: Nil,
        resolve = c => { c.ctx.addDeveloperVersionInfo(c.arg(DeveloperVersionInfoArg)) }),
      Field("removeDeveloperVersion", BooleanType,
        arguments = ServiceArg :: DeveloperVersionArg :: Nil,
        resolve = c => { c.ctx.removeDeveloperVersion(c.arg(ServiceArg), c.arg(DeveloperVersionArg)) }),
      Field("addClientVersionInfo", BooleanType,
        arguments = InstalledVersionInfoArg :: Nil,
        resolve = c => { c.ctx.addClientVersionInfo(c.arg(InstalledVersionInfoArg)) }),
      Field("removeClientVersion", BooleanType,
        arguments = ServiceArg :: ClientVersionArg :: Nil,
        resolve = c => { c.ctx.removeClientVersion(c.arg(ServiceArg), c.arg(ClientVersionArg)) }),
      Field("setDeveloperDesiredVersions", BooleanType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setDeveloperDesiredVersions(c.arg(DeveloperDesiredVersionsArg)) }),
      Field("setClientDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setClientDesiredVersions(c.arg(ClientDesiredVersionsArg)) })
  ))

  val DistributionMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("setTestedVersions", BooleanType,
        arguments = DeveloperDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setTestedVersions(c.ctx.userInfo.name, c.arg(DeveloperDesiredVersionsArg)) }),
      Field("setInstalledDesiredVersions", BooleanType,
        arguments = ClientDesiredVersionsArg :: Nil,
        resolve = c => { c.ctx.setInstalledDesiredVersions(c.ctx.userInfo.name, c.arg(ClientDesiredVersionsArg)) }),
      Field("setServicesState", BooleanType,
        arguments = InstancesStateArg :: Nil,
        resolve = c => { c.ctx.setServicesState(c.ctx.userInfo.name, c.arg(InstancesStateArg)) }))
  )

  val ServiceMutations = ObjectType(
    "Mutation",
    fields[GraphqlContext, Unit](
      Field("setServicesState", BooleanType,
        arguments = InstancesStateArg :: Nil,
        resolve = c => { c.ctx.setServicesState(c.ctx.distributionName, c.arg(InstancesStateArg)) }),
      Field("addServiceLogs", BooleanType,
        arguments = ServiceArg :: InstanceArg :: DirectoryArg :: LogLinesArg :: Nil,
        resolve = c => { c.ctx.addServiceLogs(c.ctx.distributionName, c.arg(ServiceArg), c.arg(InstanceArg), c.arg(DirectoryArg), c.arg(LogLinesArg)) }))
  )

  val AdministratorSchemaDefinition = Schema(query = AdministratorQueries, mutation = Some(AdministratorMutations))

  val DistributionSchemaDefinition = Schema(query = DistributionQueries, mutation = Some(DistributionMutations))

  val ServiceSchemaDefinition = Schema(query = ServiceQueries, mutation = Some(ServiceMutations))

  def SchemaDefinition(userRole: UserRole): Schema[GraphqlContext, Unit] = {
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