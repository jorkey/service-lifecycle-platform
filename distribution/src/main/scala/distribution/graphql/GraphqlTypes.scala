package distribution.graphql

import java.util.Date

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info.{ClientFaultReport, DesiredVersions, ServiceState, UpdateError, VersionInfo, VersionsInfo}
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import sangria.ast.StringValue
import sangria.schema._
import sangria.macros.derive._
import sangria.validation.Violation

object GraphqlTypes {
  private case object DateCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing Date"
  }

  implicit val GraphQLDate = ScalarType[Date](
    "Date",
    coerceOutput = (date, _) => date.toString,
    coerceInput = {
      case StringValue(value, _, _, _, _) => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    },
    coerceUserInput = {
      case value: String => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    })

  // Common

  case object BuildVersionViolation extends Violation {
    override def errorMessage: String = "Error during parsing BuildVersion"
  }

  implicit val BuildVersionType = ScalarType[BuildVersion]("BuildVersion",
    coerceOutput = (version, _) => version.toString,
    coerceInput = {
      case StringValue(version, _, _ , _ , _) => Right(BuildVersion.parse(version))
      case _ => Left(BuildVersionViolation)
    },
    coerceUserInput = {
      case version: String => Right(BuildVersion.parse(version))
      case _ => Left(BuildVersionViolation)
    })

  implicit val VersionInfoType = deriveObjectType[Unit, VersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, VersionsInfo]()

  case class ServiceVersion(serviceName: ServiceName, buildVersion: BuildVersion)
  implicit val ServiceVersionType = deriveObjectType[Unit, ServiceVersion]()
  implicit val DesiredVersionsType = ObjectType.apply[Unit, DesiredVersions]("DesiredVersions",
    fields[Unit, DesiredVersions](
      Field("desiredVersions", ListType(ServiceVersionType), resolve = c => {
        c.value.desiredVersions.map(entry => ServiceVersion(entry._1, entry._2)).toSeq
      })
    )
  )

  implicit val ClientInfoType = deriveObjectType[Unit, ClientInfo]()
  implicit val ClientConfigInfoType = deriveObjectType[Unit, ClientConfig]()
  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()

  // Fault

  implicit val ClientFaultReportType = deriveObjectType[Unit, ClientFaultReport]()
}