package distribution.graphql

import java.util.Date

import com.vyulabs.update.common.Common.{InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.config.ClientInfo
import com.vyulabs.update.info.{ClientFaultReport, InstanceVersions, ServiceState, UpdateError}
import com.vyulabs.update.users.UserInfo
import com.vyulabs.update.users.UserRole
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import sangria.ast.StringValue
import sangria.schema._
import sangria.macros.derive._
import sangria.validation.Violation

object GraphqlSchema {
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

  implicit val ClientInfoType = deriveObjectType[Unit, ClientInfo]()
  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val BuildVersionType = deriveObjectType[Unit, BuildVersion]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()

  // Fault

  implicit val ClientFaultReportType = deriveObjectType[Unit, ClientFaultReport]()
}