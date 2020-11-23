package distribution.graphql

import java.util.Date

import com.vyulabs.update.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.info.{BuildInfo, ClientDesiredVersion, ClientVersionInfo, DeveloperDesiredVersion, DeveloperVersionInfo, DeveloperVersionsInfo, DirectoryServiceState, DistributionFaultReport, DistributionServiceState, FaultInfo, InstallInfo, InstanceServiceState, LogLine, ServiceFaultReport, ServiceState, UpdateError}
import distribution.users.UserInfo
import distribution.users.UserRole
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.utils.Utils.serializeISO8601Date
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import distribution.mongo.InstalledDesiredVersionsDocument
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

  case object VersionViolation extends Violation {
    override def errorMessage: String = "Error during parsing version"
  }

  implicit val DeveloperVersionType = ScalarType[DeveloperVersion]("DeveloperVersion",
    coerceOutput = (version, _) => version.toString,
    coerceInput = {
      case StringValue(version, _, _ , _ , _) => Right(DeveloperVersion.parse(version))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case version: String => Right(DeveloperVersion.parse(version))
      case _ => Left(VersionViolation)
    })

  implicit val DeveloperDistributionVersionType = ScalarType[DeveloperDistributionVersion]("DeveloperDistributionVersion",
    coerceOutput = (version, _) => version.toString,
    coerceInput = {
      case StringValue(version, _, _ , _ , _) => Right(DeveloperDistributionVersion.parse(version))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case version: String => Right(DeveloperDistributionVersion.parse(version))
      case _ => Left(VersionViolation)
    })

  implicit val ClientVersionType = ScalarType[ClientVersion]("ClientVersion",
    coerceOutput = (version, _) => version.toString,
    coerceInput = {
      case StringValue(version, _, _ , _ , _) => Right(ClientVersion.parse(version))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case version: String => Right(ClientVersion.parse(version))
      case _ => Left(VersionViolation)
    })

  implicit val ClientDistributionVersionType = ScalarType[ClientDistributionVersion]("ClientDistributionVersion",
    coerceOutput = (version, _) => version.toString,
    coerceInput = {
      case StringValue(version, _, _ , _ , _) => Right(ClientDistributionVersion.parse(version))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case version: String => Right(ClientDistributionVersion.parse(version))
      case _ => Left(VersionViolation)
    })

  implicit val DeveloperDesiredVersionType = deriveObjectType[Unit, DeveloperDesiredVersion]()
  implicit val ClientDesiredVersionType = deriveObjectType[Unit, ClientDesiredVersion]()
  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildInfo]()
  implicit val DeveloperVersionInfoType = deriveObjectType[Unit, DeveloperVersionInfo]()
  implicit val InstallInfoType = deriveObjectType[Unit, InstallInfo]()
  implicit val ClientVersionInfoType = deriveObjectType[Unit, ClientVersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, DeveloperVersionsInfo]()
  implicit val InstalledDesiredVersionsType = deriveObjectType[Unit, InstalledDesiredVersionsDocument]()
  implicit val ClientConfigInfoType = deriveObjectType[Unit, DistributionClientConfig]()
  implicit val ClientInfoType = deriveObjectType[Unit, DistributionClientInfo]()
  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()
  implicit val DirectoryServiceStateType = deriveObjectType[Unit, DirectoryServiceState]()
  implicit val InstanceServiceStateType = deriveObjectType[Unit, InstanceServiceState]()
  implicit val ClientServiceStateType = deriveObjectType[Unit, DistributionServiceState]()
  implicit val FaultInfoType = deriveObjectType[Unit, FaultInfo]()
  implicit val ServiceFaultReportType = deriveObjectType[Unit, ServiceFaultReport]()
  implicit val DistributionFaultReportType = deriveObjectType[Unit, DistributionFaultReport]()

  implicit val BuildInfoInputType = deriveInputObjectType[BuildInfo](InputObjectTypeName("BuildInfoInput"))
  implicit val InstallInfoInputType = deriveInputObjectType[InstallInfo](InputObjectTypeName("InstallInfoInput"))
  implicit val DeveloperVersionInfoInputType = deriveInputObjectType[DeveloperVersionInfo](InputObjectTypeName("DeveloperVersionInfoInput"))
  implicit val InstalledVersionInfoInputType = deriveInputObjectType[ClientVersionInfo](InputObjectTypeName("InstalledVersionInfoInput"))
  implicit val BuildVersionInfoInputType = deriveInputObjectType[BuildInfo](InputObjectTypeName("BuildVersionInfoInput"))
  implicit val DeveloperDesiredVersionInfoInputType = deriveInputObjectType[DeveloperDesiredVersion](InputObjectTypeName("DeveloperDesiredVersionInput"))
  implicit val ClientDesiredVersionInfoInputType = deriveInputObjectType[ClientDesiredVersion](InputObjectTypeName("ClientDesiredVersionInput"))
  implicit val UpdateErrorInputType = deriveInputObjectType[UpdateError](InputObjectTypeName("UpdateErrorInput"))
  implicit val ServiceStateInputType = deriveInputObjectType[ServiceState](InputObjectTypeName("ServiceStateInput"))
  implicit val InstanceServiceStateInputType = deriveInputObjectType[InstanceServiceState](InputObjectTypeName("InstanceServiceStateInput"))
  implicit val LogLineInputType = deriveInputObjectType[LogLine](InputObjectTypeName("LogLineInput"))
  implicit val FaultInfoInputType = deriveInputObjectType[FaultInfo]((InputObjectTypeName("FaultInfo")))
  implicit val ServiceFaultReportInputType = deriveInputObjectType[ServiceFaultReport]((InputObjectTypeName("ServiceFaultReport")))
}