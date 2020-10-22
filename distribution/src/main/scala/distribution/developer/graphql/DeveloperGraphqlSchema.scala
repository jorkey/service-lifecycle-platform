package distribution.developer.graphql

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.users.UserRole.UserRole
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.utils.{ClientsUtils, StateUtils, VersionUtils}
import distribution.graphql.GraphqlContext
import distribution.graphql.GraphqlTypes._
import distribution.utils.{CommonUtils, GetUtils, PutUtils}

import scala.concurrent.ExecutionContext
import sangria.marshalling.sprayJson._
import sangria.schema._

case class DeveloperGraphqlContext(config: DeveloperDistributionConfig, dir: DeveloperDistributionDirectory,
                                   collections: DeveloperDatabaseCollections, userInfo: UserInfo)
                                  (implicit protected val system: ActorSystem,
                                   protected val materializer: Materializer,
                                   protected val executionContext: ExecutionContext,
                                   protected val filesLocker: SmartFilesLocker) extends GraphqlContext
  with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils {}

object DeveloperGraphqlSchema {
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  // Arguments

  val Client = Argument("client", StringType)
  val Instance = Argument("instance", StringType)
  val Directory = Argument("directory", StringType)
  val Service = Argument("service", StringType)
  val Version = Argument("version", BuildVersionType)

  val BuildInfo = Argument("buildInfo", BuildVersionInfoInputType)

  val OptionClient = Argument("client", OptionInputType(StringType))
  val OptionInstance = Argument("instance", OptionInputType(StringType))
  val OptionDirectory = Argument("directory", OptionInputType(StringType))
  val OptionService = Argument("service", OptionInputType(StringType))
  val OptionVersion = Argument("version", OptionInputType(BuildVersionType))
  val OptionLast = Argument("last", OptionInputType(IntType))
  val OptionMerged = Argument("merged", OptionInputType(BooleanType))

  // Queries

  val CommonQueries = fields[DeveloperGraphqlContext, Unit](
    Field("userInfo", fieldType = UserInfoType,
      resolve = c => { c.ctx.userInfo }),
  )

  val AdministratorQueries = ObjectType(
    "Query",
     CommonQueries ++ fields[DeveloperGraphqlContext, Unit](
       Field("ownServiceVersion", BuildVersionType,
         arguments = Service :: Directory :: Nil,
         resolve = c => { c.ctx.getServiceVersion(c.arg(Service), new File(c.arg(Directory))) }),
       Field("versionsInfo", ListType(VersionInfoType),
         arguments = Service :: OptionClient :: OptionVersion :: Nil,
         resolve = c => { c.ctx.getVersionsInfo(c.arg(Service), clientName = c.arg(OptionClient), version = c.arg(OptionVersion)) }),
       Field("clientsInfo", ListType(ClientInfoType),
         resolve = c => c.ctx.getClientsInfo()),
       Field("desiredVersions", DesiredVersionsType,
         arguments = OptionClient :: OptionMerged :: Nil,
         resolve = c => { c.ctx.getDesiredVersions(c.arg(OptionClient), c.arg(OptionMerged).getOrElse(false)) }),
       Field("installedVersions", DesiredVersionsType,
         arguments = Client :: Nil,
         resolve = c => { c.ctx.getInstalledVersions(c.arg(Client)) }),
       Field("servicesState", ListType(ClientServiceStateType),
         arguments = OptionClient :: OptionService :: OptionInstance :: OptionDirectory :: Nil,
         resolve = c => { c.ctx.getServicesState(c.arg(OptionClient), c.arg(OptionService), c.arg(OptionInstance), c.arg(OptionDirectory)) }),
       Field("faultReports", ListType(ClientFaultReportType),
         arguments = OptionClient :: OptionService :: OptionLast :: Nil,
         resolve = c => { c.ctx.getClientFaultReports(c.arg(OptionClient), c.arg(OptionService), c.arg(OptionLast)) })
     ))

  val ClientQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[DeveloperGraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", DesiredVersionsType,
        resolve = c => { c.ctx.getClientDesiredVersions(c.ctx.userInfo.name) }),
      Field("desiredVersion", BuildVersionType,
        arguments = Service :: Nil,
        resolve = c => { c.ctx.getDesiredVersion(c.arg(Service), c.ctx.getClientDesiredVersions(c.ctx.userInfo.name)) }),
    )
  )

  // Mutations

  val CommonMutations = fields[DeveloperGraphqlContext, Unit](
    Field("addVersionInfo", VersionInfoType,
      arguments = Service :: Version :: BuildInfo :: Nil,
      resolve = c => { c.ctx.versionInfoUpload(c.arg(Service), c.arg(Version), c.arg(BuildInfo)) })
  )

  val AdministratorMutations = ObjectType(
    "Mutation",
    CommonMutations)

  val AdministratorSchemaDefinition = Schema(query = AdministratorQueries, mutation = Some(AdministratorMutations))
  val ClientSchemaDefinition = Schema(query = ClientQueries)

  def SchemaDefinition(userRole: UserRole): Schema[DeveloperGraphqlContext, Unit] = {
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