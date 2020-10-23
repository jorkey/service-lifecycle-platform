package distribution.graphql

import java.util.Date

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info.{BuildVersionInfo, ClientFaultReport, ClientServiceState, DesiredVersions, DesiredVersionsMap, DirectoryServiceState, InstanceServiceState, ServiceState, ServiceVersion, UpdateError, VersionInfo, VersionsInfo}
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.utils.Utils.serializeISO8601Date
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
    coerceOutput = (date, _) => serializeISO8601Date(date),
    coerceInput = {
      case StringValue(value, _, _, _, _) => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    },
    coerceUserInput = {
      case value: String => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    })

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

  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildVersionInfo]()
  implicit val VersionInfoType = deriveObjectType[Unit, VersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, VersionsInfo]()
  implicit val ServiceVersionType = deriveObjectType[Unit, ServiceVersion]()
  implicit val DesiredVersionsType = deriveObjectType[Unit, DesiredVersions]()
  implicit val ClientConfigInfoType = deriveObjectType[Unit, ClientConfig]()
  implicit val ClientInfoType = deriveObjectType[Unit, ClientInfo]()
  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()
  implicit val DirectoryServiceStateType = deriveObjectType[Unit, DirectoryServiceState]()
  implicit val InstanceServiceStateType = deriveObjectType[Unit, InstanceServiceState]()
  implicit val ClientServiceStateType = deriveObjectType[Unit, ClientServiceState]()

  implicit val ClientFaultReportType = deriveObjectType[Unit, ClientFaultReport]()

  implicit val BuildVersionInfoInputType = deriveInputObjectType[BuildVersionInfo](
    InputObjectTypeName("BuildVersionInfoInput")
  )
  implicit val ServiceVersionInfoInputType = deriveInputObjectType[ServiceVersion](
    InputObjectTypeName("ServiceVersionInput")
  )
  implicit val DesiredVersionsInputType = deriveInputObjectType[DesiredVersions](
    InputObjectTypeName("DesiredVersionsInput")
  )
}