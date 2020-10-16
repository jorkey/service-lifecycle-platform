package distribution.developer.graphql

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.info.{ClientFaultReport, InstanceVersions, VersionInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.users.UserRole.UserRole
import com.vyulabs.update.version.BuildVersion
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.utils.{ClientsUtils, StateUtils}
import distribution.graphql.{AuthorizationException, GraphqlContext, NotFoundException}
import distribution.graphql.GraphqlTypes._
import distribution.mongo.MongoDb
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}
import sangria.macros.derive.deriveObjectType

import collection.JavaConverters._
import scala.concurrent.ExecutionContext
import sangria.schema._

case class DeveloperGraphqlContext(config: DeveloperDistributionConfig, dir: DeveloperDistributionDirectory, mongoDb: MongoDb, userInfo: UserInfo)
                                  (implicit protected val system: ActorSystem,
                                   protected val materializer: Materializer,
                                   protected val executionContext: ExecutionContext,
                                   protected val filesLocker: SmartFilesLocker) extends GraphqlContext
  with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils {}

object DeveloperGraphqlSchema {
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  // Versions

  case class DirectoryInstances(directory: ServiceDirectory, instances: Seq[InstanceId])
  implicit val DirectoryInstancesType = deriveObjectType[Unit, DirectoryInstances]()

  case class VersionDirectories(version: BuildVersion, directories: Seq[DirectoryInstances])
  object VersionDirectories { def apply(version: BuildVersion, directories: Map[ServiceDirectory, Set[InstanceId]]) =
    new VersionDirectories(version, directories.map(entry => DirectoryInstances(entry._1, entry._2.toSeq)).toSeq) }
  implicit val VersionDirectoriesType = deriveObjectType[Unit, VersionDirectories]()

  case class ServiceVersions(service: ServiceName, versions: Seq[VersionDirectories])
  object ServiceVersions { def apply(service: ServiceName, versions: Map[BuildVersion, Map[ServiceDirectory, Set[InstanceId]]]) =
    new ServiceVersions(service, versions.map(entry => VersionDirectories.apply(entry._1,
      entry._2.map(entry => DirectoryInstances(entry._1, entry._2.toSeq)).toSeq)).toSeq) }
  implicit val ServiceVersionsType = deriveObjectType[Unit, ServiceVersions]()

  implicit val InstanceVersionsType = ObjectType.apply[Unit, InstanceVersions]("InstanceVersions",
    fields[Unit, InstanceVersions](
      Field("versions", ListType(ServiceVersionsType), resolve = c => {
        c.value.versions.map(entry => ServiceVersions(entry._1, entry._2)).toSeq
      })
    )
  )

  // Arguments

  val Client = Argument("client", StringType)
  val Instance = Argument("instance", StringType)
  val Directory = Argument("directory", StringType)
  val Service = Argument("service", StringType)
  val Version = Argument("version", BuildVersionType)

  val OptionClient = Argument("client", OptionInputType(StringType))
  val OptionService = Argument("service", OptionInputType(StringType))
  val OptionLast = Argument("last", OptionInputType(IntType))

  // Queries

  val CommonQueries = fields[DeveloperGraphqlContext, Unit](
    Field("userInfo", fieldType = UserInfoType,
      resolve = c => { c.ctx.userInfo }),
  )

  val AdministratorQueries = ObjectType(
    "Query",
     CommonQueries ++ fields[DeveloperGraphqlContext, Unit](
       Field("versionInfo", VersionInfoType,
         arguments = Service :: Version :: Nil,
         resolve = c => { c.ctx.parseJsonFileWithLock[VersionInfo](c.ctx.dir.getVersionInfoFile(c.arg(Service), c.arg(Version)))
           .map(_.getOrElse(throw NotFoundException())) }),
       Field("versionsInfo", ListType(VersionInfoType),
         arguments = Service :: OptionClient :: Nil,
         resolve = c => { c.ctx.getVersionsInfo(c.ctx.dir.getServiceDir(c.arg(Service), c.arg(OptionClient))) }),
       Field("desiredVersions", DesiredVersionsType,
         arguments = OptionClient :: Nil,
         resolve = c => { c.ctx.getDesiredVersions(c.arg(OptionClient)).map(_.getOrElse(throw NotFoundException())) }),
       Field("desiredVersion", BuildVersionType,
         arguments = Service :: Nil,
         resolve = c => { c.ctx.getDesiredVersion(c.arg(Service), c.ctx.getDesiredVersions(None)).map(_.getOrElse(throw NotFoundException())) }),
       Field("ownServiceVersion", BuildVersionType,
         arguments = Service :: Nil,
         resolve = c => { c.ctx.getServiceVersion(c.arg(Service), new File(c.arg(Directory))).getOrElse(throw NotFoundException()) }),
       Field("clientsInfo", ListType(ClientInfoType),
         resolve = c => c.ctx.getClientsInfo()),
       Field("mergedDesiredVersions", DesiredVersionsType,
         arguments = Client :: Nil,
         resolve = c => { c.ctx.getClientDesiredVersions(c.arg(Client)).map(_.getOrElse(throw NotFoundException())) }),
       Field("installedDesiredVersions", DesiredVersionsType,
         arguments = Client :: Nil,
         resolve = c => { c.ctx.getInstalledDesiredVersions(c.arg(Client)).map(_.getOrElse(throw NotFoundException())) }),
       Field("instanceVersions", InstanceVersionsType,
         arguments = Client :: Nil,
         resolve = c => { c.ctx.getClientInstanceVersions(c.arg(Client)) }),
       Field("serviceState", ServiceStateType,
         arguments = Client :: Instance :: Directory :: Service :: Nil,
         resolve = c => { c.ctx.getServiceState(c.arg(Client), c.arg(Instance), c.arg(Directory), c.arg(Service))
           .map(_.getOrElse(throw NotFoundException())) }),
       Field("faultReports", ListType(ClientFaultReportType),
         arguments = OptionClient :: OptionService :: OptionLast :: Nil,
         resolve = c => {
           val clientArg = c.arg(OptionClient).map { client => Filters.eq("clientName", client) }
           val serviceArg = c.arg(OptionService).map { service => Filters.eq("serviceName", service) }
           val filters = Filters.and((clientArg ++ serviceArg).asJava)
           // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
           val sort = c.arg(OptionLast).map { last => Sorts.descending("_id") }
           for {
             collection <- c.ctx.mongoDb.getCollection[ClientFaultReport]("faults")
             faults <- collection.find(filters, sort, c.arg(OptionLast))
           } yield faults
         })
     ))

  val ClientQueries = ObjectType(
    "Query",
    CommonQueries ++ fields[DeveloperGraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", DesiredVersionsType,
        resolve = c => { c.ctx.getClientDesiredVersions(c.ctx.userInfo.name).map(_.getOrElse(throw NotFoundException())) }),
      Field("desiredVersion", BuildVersionType,
        arguments = Service :: Nil,
        resolve = c => { c.ctx.getDesiredVersion(c.arg(Service), c.ctx.getClientDesiredVersions(c.ctx.userInfo.name))
          .map(_.getOrElse(throw NotFoundException())) }),
    )
  )

  val AdministratorSchemaDefinition = Schema(query = AdministratorQueries)
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