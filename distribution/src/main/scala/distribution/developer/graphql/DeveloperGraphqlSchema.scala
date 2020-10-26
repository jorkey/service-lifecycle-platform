package distribution.developer.graphql

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.distribution.developer.DeveloperDistributionDirectory
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.users.{UserInfo, UserRole}
import com.vyulabs.update.users.UserRole.UserRole
import distribution.developer.DeveloperDatabaseCollections
import distribution.developer.config.DeveloperDistributionConfig
import distribution.developer.utils.{ClientsUtils, StateUtils, VersionUtils}
import distribution.graphql.{GraphqlContext, NotFoundException}
import distribution.graphql.GraphqlTypes._
import distribution.utils.{CommonUtils, GetUtils, PutUtils}

import scala.concurrent.ExecutionContext
import sangria.schema._

class DeveloperGraphqlContext(override val config: DeveloperDistributionConfig,
                              override val dir: DeveloperDistributionDirectory,
                              override val collections: DeveloperDatabaseCollections,
                              userInfo: UserInfo)
                            (implicit system: ActorSystem, materializer: Materializer,
                             executionContext: ExecutionContext, filesLocker: SmartFilesLocker)
    extends GraphqlContext(config, dir, collections, userInfo)
  with ClientsUtils with StateUtils with GetUtils with PutUtils with VersionUtils with CommonUtils {}

object DeveloperGraphqlSchema {
  import distribution.graphql.GraphqlSchema._

  // Queries

  val AdministratorQueries = ObjectType(
    "Query",
    CommonAdministratorQueries[DeveloperGraphqlContext] ++ fields[DeveloperGraphqlContext, Unit](
       Field("clientsInfo", ListType(ClientInfoType),
         resolve = c => c.ctx.getClientsInfo()),
       Field("clientDesiredVersions", ListType(DesiredVersionType),
         arguments = ClientArg :: OptionServicesArg :: OptionMergedArg :: Nil,
         resolve = c => { c.ctx.getClientDesiredVersions(c.arg(ClientArg),
           c.arg(OptionServicesArg).getOrElse(Seq.empty).toSet, c.arg(OptionMergedArg).getOrElse(false)) }),
       Field("installedVersions", ListType(DesiredVersionType),
         arguments = ClientArg :: Nil,
         resolve = c => { c.ctx.getInstalledVersions(c.arg(ClientArg)) }),
       Field("servicesState", ListType(ClientServiceStateType),
         arguments = OptionClientArg :: OptionServiceArg :: OptionInstanceArg :: OptionDirectoryArg :: Nil,
         resolve = c => { c.ctx.getServicesState(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionInstanceArg), c.arg(OptionDirectoryArg)) }),
       Field("faultReports", ListType(ClientFaultReportType),
         arguments = OptionClientArg :: OptionServiceArg :: OptionLastArg :: Nil,
         resolve = c => { c.ctx.getClientFaultReports(c.arg(OptionClientArg), c.arg(OptionServiceArg), c.arg(OptionLastArg)) })
     ))

  val ClientQueries = ObjectType(
    "Query",
    CommonQueries[DeveloperGraphqlContext] ++ fields[DeveloperGraphqlContext, Unit](
      Field("config", ClientConfigInfoType,
        resolve = c => { c.ctx.getClientConfig(c.ctx.userInfo.name) }),
      Field("desiredVersions", ListType(DesiredVersionType),
        resolve = c => { c.ctx.getClientDesiredVersions(c.ctx.userInfo.name) }),
      Field("desiredVersion", BuildVersionType,
        arguments = ServiceArg :: Nil,
        resolve = c => { c.ctx.getClientDesiredVersions(c.ctx.userInfo.name).map(_.map(_.buildVersion)
          .headOption.getOrElse(throw NotFoundException(s"Desired version is not found"))) }),
    )
  )

  // Mutations

  val AdministratorMutations = ObjectType(
    "Mutation",
    CommonAdministratorMutations[DeveloperGraphqlContext])

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