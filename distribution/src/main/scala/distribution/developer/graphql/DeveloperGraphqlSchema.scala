package distribution.developer.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.info.{ClientFaultReport, InstanceVersions}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.version.BuildVersion
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.utils.{ClientsUtils, StateUtils}
import distribution.graphql.GraphqlContext
import distribution.graphql.GraphqlSchema._
import distribution.mongo.MongoDb
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}
import sangria.macros.derive.deriveObjectType

import collection.JavaConverters._
import scala.concurrent.ExecutionContext
import sangria.schema._

case class DeveloperGraphqlContext(config: DeveloperDistributionConfig, dir: DeveloperDistributionDirectory, mongoDb: MongoDb, userInfo: Option[UserInfo])
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

  val Client = Argument("client", StringType)

  val OptionClient = Argument("client", OptionInputType(StringType))
  val OptionService = Argument("service", OptionInputType(StringType))
  val OptionLast = Argument("last", OptionInputType(IntType))

  val QueryType = ObjectType(
    "Query",
    fields[DeveloperGraphqlContext, Unit](
      /*Field("user", fieldType = UserInfoType,
        resolve = c => {
          c.ctx.dir.
        }

        ),*/
      Field("clients", ListType(ClientInfoType),
        resolve = c => c.ctx.getClientsInfo()),
      Field("instanceVersions", InstanceVersionsType,
        arguments = Client :: Nil,
        resolve = c => { c.ctx.getClientInstanceVersions(c.arg(Client)) }),
      Field("faults", ListType(ClientFaultReportType),
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
    )
  )

  val SchemaDefinition = Schema(query = QueryType)
}