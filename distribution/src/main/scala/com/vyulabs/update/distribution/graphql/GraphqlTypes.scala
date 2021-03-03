package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.common.config.{DistributionClientConfig, DistributionClientInfo}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.utils.Utils.serializeISO8601Date
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.schema._
import sangria.validation.Violation

import java.util.Date

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

  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()

  implicit val DeveloperDesiredVersionType = deriveObjectType[Unit, DeveloperDesiredVersion]()
  implicit val ClientDesiredVersionType = deriveObjectType[Unit, ClientDesiredVersion]()
  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildInfo]()
  implicit val DeveloperVersionInfoType = deriveObjectType[Unit, DeveloperVersionInfo]()
  implicit val InstallInfoType = deriveObjectType[Unit, InstallInfo]()
  implicit val ClientVersionInfoType = deriveObjectType[Unit, ClientVersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, DeveloperVersionsInfo]()
  implicit val InstalledDesiredVersionsType = deriveObjectType[Unit, InstalledDesiredVersions]()
  implicit val ClientConfigInfoType = deriveObjectType[Unit, DistributionClientConfig]()
  implicit val ClientInfoType = deriveObjectType[Unit, DistributionClientInfo]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()
  implicit val DirectoryServiceStateType = deriveObjectType[Unit, DirectoryServiceState]()
  implicit val InstanceServiceStateType = deriveObjectType[Unit, InstanceServiceState]()
  implicit val ClientServiceStateType = deriveObjectType[Unit, DistributionServiceState]()
  implicit val LogLineType = deriveObjectType[Unit, LogLine]()
  implicit val ServiceLogLineType = deriveObjectType[Unit, ServiceLogLine]()
  implicit val SequencedServiceLogLineType = deriveObjectType[Unit, SequencedServiceLogLine]()
  implicit val FaultInfoType = deriveObjectType[Unit, FaultInfo]()
  implicit val ServiceFaultReportType = deriveObjectType[Unit, ServiceFaultReport]()
  implicit val DistributionFaultReportType = deriveObjectType[Unit, DistributionFaultReport]()

  implicit val BuildInfoInputType = deriveInputObjectType[BuildInfo](InputObjectTypeName("BuildInfoInput"))
  implicit val InstallInfoInputType = deriveInputObjectType[InstallInfo](InputObjectTypeName("InstallInfoInput"))
  implicit val DeveloperVersionInfoInputType = deriveInputObjectType[DeveloperVersionInfo](InputObjectTypeName("DeveloperVersionInfoInput"))
  implicit val ClientVersionInfoInputType = deriveInputObjectType[ClientVersionInfo](InputObjectTypeName("ClientVersionInfoInput"))
  implicit val BuildVersionInfoInputType = deriveInputObjectType[BuildInfo](InputObjectTypeName("BuildVersionInfoInput"))
  implicit val DeveloperDesiredVersionInputType = deriveInputObjectType[DeveloperDesiredVersion](InputObjectTypeName("DeveloperDesiredVersionInput"))
  implicit val ClientDesiredVersionInputType = deriveInputObjectType[ClientDesiredVersion](InputObjectTypeName("ClientDesiredVersionInput"))
  implicit val DeveloperDesiredVersionDeltaInputType = deriveInputObjectType[DeveloperDesiredVersionDelta](InputObjectTypeName("DeveloperDesiredVersionDeltaInput"))
  implicit val ClientDesiredVersionDeltaInputType = deriveInputObjectType[ClientDesiredVersionDelta](InputObjectTypeName("ClientDesiredVersionDeltaInput"))
  implicit val UpdateErrorInputType = deriveInputObjectType[UpdateError](InputObjectTypeName("UpdateErrorInput"))
  implicit val ServiceStateInputType = deriveInputObjectType[ServiceState](InputObjectTypeName("ServiceStateInput"))
  implicit val InstanceServiceStateInputType = deriveInputObjectType[InstanceServiceState](InputObjectTypeName("InstanceServiceStateInput"))
  implicit val LogLineInputType = deriveInputObjectType[LogLine](InputObjectTypeName("LogLineInput"))
  implicit val FaultInfoInputType = deriveInputObjectType[FaultInfo](InputObjectTypeName("FaultInfoInput"))
  implicit val ServiceFaultReportInputType = deriveInputObjectType[ServiceFaultReport](InputObjectTypeName("ServiceFaultReportInput"))
}