package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.common.accounts._
import com.vyulabs.update.common.config.{BuildServiceConfig, GitConfig, NamedStringValue, Repository}
import com.vyulabs.update.common.info._
import com.vyulabs.update.common.utils.JsonFormats.FiniteDurationFormat
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.graphql.utils.{SequencedTaskInfo, TaskParameter}
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import sangria.ast
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.schema.{ScalarType, valueOutput}
import sangria.validation.Violation
import spray.json._

import java.net.URL
import java.util.Date
import scala.concurrent.duration.FiniteDuration

object GraphqlTypes {
  private case object DateCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing Date"
  }

  implicit val DateType = ScalarType[Date]("Date",
    coerceOutput = (date, _) => date.getTime,
    coerceInput = {
      case ast.BigIntValue(value, _, _) => Right(new Date(value.longValue()))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case i: Long => Right(new Date(i))
      case i: BigInt => Right(new Date(i.longValue()))
      case d: BigDecimal if d.isWhole => Right(new Date(d.longValue()))
      case _ => Left(VersionViolation)
    })

  implicit val BigintType = ScalarType[BigInt]("BigInt",
    description = Some(
      "The `BigInt` scalar type represents non-fractional signed whole numeric values. " +
        "BigInt can represent arbitrary big values."),
    coerceOutput = valueOutput,
    coerceInput = {
      case ast.IntValue(i, _, _) => Right(i)
      case ast.BigIntValue(i, _, _) => Right(i)
      case StringValue(value, _, _, _, _) => Right(BigInt(value))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case i: Int => Right(BigInt(i))
      case i: Long => Right(BigInt(i))
      case i: BigInt => Right(i)
      case d: Double if d.isWhole => Right(BigInt(d.toLong))
      case d: BigDecimal if d.isWhole => Right(d.toBigInt)
      case value: String => Right(BigInt(value))
      case _ => Left(VersionViolation)
    }
  )

  implicit val FiniteDurationType = ScalarType[FiniteDuration]("FiniteDuration",
    coerceOutput = (duration, _) => duration.toJson.compactPrint,
    coerceInput = {
      case StringValue(duration, _, _ , _ , _) => Right(duration.parseJson.convertTo[FiniteDuration])
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case duration: String => Right(duration.parseJson.convertTo[FiniteDuration])
      case _ => Left(VersionViolation)
    })

  implicit val UrlType = ScalarType[URL]("URL",
    coerceOutput = (url, _) => url.toString,
    coerceInput = {
      case StringValue(url, _, _ , _ , _) => Right(new URL(url))
      case _ => Left(VersionViolation)
    },
    coerceUserInput = {
      case url: String => Right(new URL(url))
      case _ => Left(VersionViolation)
    })

  case object VersionViolation extends Violation {
    override def errorMessage: String = "Error during parsing version"
  }

  implicit val AccountRoleType = deriveEnumType[AccountRole.AccountRole]()
  implicit val BuildTargetType = deriveEnumType[BuildTarget.BuildTarget]()
  implicit val BuildStatusType = deriveEnumType[BuildStatus.BuildStatus]()

  implicit val ServicesProfileType = deriveObjectType[Unit, ServicesProfile]()
  implicit val GitConfigType = deriveObjectType[Unit, GitConfig]()
  implicit val SourceConfigType = deriveObjectType[Unit, Repository]()
  implicit val DeveloperVersionType = deriveObjectType[Unit, DeveloperVersion]()
  implicit val DistributionVersionType = deriveObjectType[Unit, DeveloperDistributionVersion]()
  implicit val ClientVersionType = deriveObjectType[Unit, ClientVersion]()
  implicit val ClientDistributionVersionType = deriveObjectType[Unit, ClientDistributionVersion]()
  implicit val DistributionInfoType = deriveObjectType[Unit, DistributionInfo]()
  implicit val DeveloperDesiredVersionType = deriveObjectType[Unit, DeveloperDesiredVersion]()
  implicit val TimedDeveloperDesiredVersionsType = deriveObjectType[Unit, TimedDeveloperDesiredVersions]()
  implicit val ClientDesiredVersionType = deriveObjectType[Unit, ClientDesiredVersion]()
  implicit val TimedClientDesiredVersionsType = deriveObjectType[Unit, TimedClientDesiredVersions]()
  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildInfo]()
  implicit val DeveloperVersionInfoType = deriveObjectType[Unit, DeveloperVersionInfo]()
  implicit val InstallInfoType = deriveObjectType[Unit, InstallInfo]()
  implicit val ClientVersionInfoType = deriveObjectType[Unit, ClientVersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, DeveloperVersionsInfo]()
  implicit val InstalledDesiredVersionsType = deriveObjectType[Unit, InstalledDesiredVersions]()
  implicit val AccountHumanInfoType = deriveObjectType[Unit, UserAccountProperties]()
  implicit val AccountConsumerInfoType = deriveObjectType[Unit, ConsumerAccountProperties]()
  implicit val UserAccountInfoType = deriveObjectType[Unit, UserAccountInfo]()
  implicit val ServiceAccountInfoType = deriveObjectType[Unit, ServiceAccountInfo]()
  implicit val ConsumerAccountInfoType = deriveObjectType[Unit, ConsumerAccountInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val BuildServiceStateType = deriveObjectType[Unit, TimedBuildServiceState]()
  implicit val InstanceStateType = deriveObjectType[Unit, InstanceState]()
  implicit val DirectoryInstanceStateType = deriveObjectType[Unit, DirectoryInstanceState]()
  implicit val AddressedInstanceStateType = deriveObjectType[Unit, AddressedInstanceState]()
  implicit val DistributionServiceStateType = deriveObjectType[Unit, DistributionInstanceState]()
  implicit val LogLineType = deriveObjectType[Unit, LogLine]()
  implicit val ServiceLogLineType = deriveObjectType[Unit, ServiceLogLine]()
  implicit val SequencedServiceLogLineType = deriveObjectType[Unit, SequencedServiceLogLine]()
  implicit val FaultInfoType = deriveObjectType[Unit, FaultInfo]()
  implicit val FileInfoType = deriveObjectType[Unit, FileInfo]()
  implicit val ServiceFaultReportType = deriveObjectType[Unit, ServiceFaultReport]()
  implicit val DistributionFaultReportType = deriveObjectType[Unit, DistributionFaultReport]()
  implicit val ProviderInfoType = deriveObjectType[Unit, DistributionProviderInfo]()
  implicit val TaskParameterType = deriveObjectType[Unit, TaskParameter]()
  implicit val SequencedTaskInfoType = deriveObjectType[Unit, SequencedTaskInfo]()
  implicit val EnvironmentVariableType = deriveObjectType[Unit, NamedStringValue]()
  implicit val BuildServiceConfigType = deriveObjectType[Unit, BuildServiceConfig]()

  implicit val UserAccountPropertiesInputType = deriveInputObjectType[UserAccountProperties](InputObjectTypeName("UserAccountPropertiesInput"))
  implicit val ConsumerAccountPropertiesInputType = deriveInputObjectType[ConsumerAccountProperties](InputObjectTypeName("ConsumerAccountPropertiesInput"))
  implicit val DeveloperVersionInputType = deriveInputObjectType[DeveloperVersion](InputObjectTypeName("DeveloperVersionInput"))
  implicit val ClientVersionInputType = deriveInputObjectType[ClientVersion](InputObjectTypeName("ClientVersionInput"))
  implicit val DeveloperDistributionVersionInputType = deriveInputObjectType[DeveloperDistributionVersion](InputObjectTypeName("DeveloperDistributionVersionInput"))
  implicit val ClientDistributionVersionInputType = deriveInputObjectType[ClientDistributionVersion](InputObjectTypeName("ClientDistributionVersionInput"))
  implicit val GitConfigInputType = deriveInputObjectType[GitConfig](InputObjectTypeName("GitConfigInput"))
  implicit val RepositoryInputType = deriveInputObjectType[Repository](InputObjectTypeName("RepositoryInput"))
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
  implicit val InstanceStateInputType = deriveInputObjectType[InstanceState](InputObjectTypeName("InstanceStateInput"))
  implicit val InstanceAddressedStateInputType = deriveInputObjectType[AddressedInstanceState](InputObjectTypeName("AddressedInstanceStateInput"))
  implicit val LogLineInputType = deriveInputObjectType[LogLine](InputObjectTypeName("LogLineInput"))
  implicit val FaultInfoInputType = deriveInputObjectType[FaultInfo](InputObjectTypeName("FaultInfoInput"))
  implicit val FileInfoInputType = deriveInputObjectType[FileInfo](InputObjectTypeName("FileInfoInput"))
  implicit val ServiceFaultReportInputType = deriveInputObjectType[ServiceFaultReport](InputObjectTypeName("ServiceFaultReportInput"))
  implicit val TaskParameterInputType = deriveInputObjectType[TaskParameter](InputObjectTypeName("TaskParameterInput"))
  implicit val NamedStringValueInputType = deriveInputObjectType[NamedStringValue](InputObjectTypeName("NamedStringValueInput"))
}