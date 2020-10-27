package distribution.graphql

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.distribution.DistributionMain.log
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.UserInfo
import distribution.DatabaseCollections
import distribution.config.DistributionConfig
import distribution.graphql.GraphqlTypes._
import distribution.utils.{CommonUtils, GetUtils, PutUtils, VersionUtils}

import scala.concurrent.ExecutionContext
import sangria.marshalling.sprayJson._
import sangria.schema._

case class GraphqlContext(config: DistributionConfig, dir: DistributionDirectory,
                         collections: DatabaseCollections, userInfo: UserInfo)
                        (implicit protected val system: ActorSystem,
                         protected val materializer: Materializer,
                         protected val executionContext: ExecutionContext,
                         protected val filesLocker: SmartFilesLocker) extends GetUtils with PutUtils with VersionUtils with CommonUtils {}

object GraphqlSchema {
  implicit val executionContext = ExecutionContext.fromExecutor(null, ex => log.error("Uncatched exception", ex))

  // Arguments

  val ClientArg = Argument("client", StringType)
  val InstanceArg = Argument("instance", StringType)
  val DirectoryArg = Argument("directory", StringType)
  val ServiceArg = Argument("service", StringType)
  val VersionArg = Argument("version", BuildVersionType)
  val BuildInfoArg = Argument("buildInfo", BuildVersionInfoInputType)
  val DesiredVersionsArg = Argument("versions", ListInputType(DesiredVersionInfoInputType))

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

  def CommonAdministratorQueries[T <: GraphqlContext] = CommonQueries[T] ++ fields[T, Unit](
    Field("ownServiceVersion", BuildVersionType,
      arguments = ServiceArg :: DirectoryArg :: Nil,
      resolve = c => { c.ctx.getServiceVersion(c.arg(ServiceArg), new File(c.arg(DirectoryArg))) }),
    Field("versionsInfo", ListType(VersionInfoType),
      arguments = ServiceArg :: OptionClientArg :: OptionVersionArg :: Nil,
      resolve = c => { c.ctx.getVersionsInfo(c.arg(ServiceArg), clientName = c.arg(OptionClientArg), version = c.arg(OptionVersionArg)) }),
    Field("desiredVersions", ListType(DesiredVersionType),
      arguments = OptionServicesArg :: Nil,
      resolve = c => { c.ctx.getDesiredVersions(c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet) })
  )

  // Mutations

  def CommonAdministratorMutations[T <: GraphqlContext] = fields[T, Unit](
    Field("addVersionInfo", VersionInfoType,
      arguments = ServiceArg :: VersionArg :: BuildInfoArg :: Nil,
      resolve = c => { c.ctx.addVersionInfo(c.arg(ServiceArg), c.arg(VersionArg), c.arg(BuildInfoArg)) }),
    Field("removeVersion", BooleanType,
      arguments = ServiceArg :: VersionArg :: Nil,
      resolve = c => { c.ctx.removeVersion(c.arg(ServiceArg), c.arg(VersionArg)) }),
    Field("desiredVersions", BooleanType,
      arguments = DesiredVersionsArg :: Nil,
      resolve = c => { c.ctx.setDesiredVersions(c.arg(DesiredVersionsArg)) })
  )

  val AdministratorSchemaDefinition = Schema(query = ObjectType("Query", CommonAdministratorQueries[GraphqlContext]),
    mutation = Some(ObjectType("Mutation", CommonAdministratorMutations[GraphqlContext])))
}