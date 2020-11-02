package distribution.graphql

import java.util.Date

import com.vyulabs.update.config.{ClientConfig, ClientInfo}
import com.vyulabs.update.info.{BuildInfo, ClientFaultReport, ClientServiceState, DesiredVersion, DirectoryServiceState, InstanceServiceState, ServiceState, UpdateError, DeveloperVersionInfo, DeveloperVersionsInfo}
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.utils.Utils.serializeISO8601Date
import com.vyulabs.update.version.BuildVersion
import sangria.ast.StringValue
import sangria.schema._
import sangria.macros.derive._
import sangria.marshalling.{InputUnmarshaller, ToInput}
import sangria.validation.Violation
import spray.json.{JsNull, JsValue, enrichAny}

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

  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildInfo]()
  implicit val VersionInfoType = deriveObjectType[Unit, DeveloperVersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, DeveloperVersionsInfo]()
  implicit val ServiceVersionType = deriveObjectType[Unit, DesiredVersion]()
  implicit val DesiredVersionType = deriveObjectType[Unit, DesiredVersion]()
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

  implicit val BuildVersionInfoInputType = deriveInputObjectType[BuildInfo](InputObjectTypeName("BuildVersionInfoInput"))
  implicit val DesiredVersionInfoInputType = deriveInputObjectType[DesiredVersion](InputObjectTypeName("DesiredVersionInput"))
  implicit val UpdateErrorInputType = deriveInputObjectType[UpdateError](InputObjectTypeName("UpdateErrorInput"))
  implicit val ServiceStateInputType = deriveInputObjectType[ServiceState](InputObjectTypeName("ServiceStateInput"))
  implicit val InstanceServiceStateInputType = deriveInputObjectType[InstanceServiceState](InputObjectTypeName("InstanceServiceStateInput"))
}