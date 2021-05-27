package com.vyulabs.update.distribution.graphql

import com.vyulabs.update.common.config.{GitConfig, SourceConfig}
import com.vyulabs.update.common.info.{DistributionProviderInfo, _}
import com.vyulabs.update.common.utils.JsonFormats.FiniteDurationFormat
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.common.utils.Utils.serializeISO8601Date
import com.vyulabs.update.common.version.{ClientDistributionVersion, ClientVersion, DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.distribution.mongo.InstalledDesiredVersions
import sangria.ast.StringValue
import sangria.macros.derive._
import sangria.schema._
import sangria.validation.Violation
import spray.json._

import java.net.URL
import java.util.Date
import scala.concurrent.duration.FiniteDuration

object GraphqlTypes {
  private case object DateCoerceViolation extends Violation {
    override def errorMessage: String = "Error during parsing Date"
  }

  implicit val GraphQLDate = ScalarType[Date]("Date",
    coerceOutput = (date, _) => serializeISO8601Date(date),
    coerceInput = {
      case StringValue(value, _, _, _, _) => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    },
    coerceUserInput = {
      case value: String => Utils.parseISO8601Date(value).toRight(DateCoerceViolation)
      case _ => Left(DateCoerceViolation)
    })

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

  implicit val UserRoleType = deriveEnumType[UserRole.UserRole]()

  implicit val ServicesProfileType = deriveObjectType[Unit, ServicesProfile]()
  implicit val GitConfigType = deriveObjectType[Unit, GitConfig]()
  implicit val SourceConfigType = deriveObjectType[Unit, SourceConfig]()
  implicit val DeveloperVersionType = deriveObjectType[Unit, DeveloperVersion]()
  implicit val DistributionVersionType = deriveObjectType[Unit, DeveloperDistributionVersion]()
  implicit val ClientVersionType = deriveObjectType[Unit, ClientVersion]()
  implicit val ClientDistributionVersionType = deriveObjectType[Unit, ClientDistributionVersion]()
  implicit val DistributionInfoType = deriveObjectType[Unit, DistributionInfo]()
  implicit val DeveloperDesiredVersionType = deriveObjectType[Unit, DeveloperDesiredVersion]()
  implicit val ClientDesiredVersionType = deriveObjectType[Unit, ClientDesiredVersion]()
  implicit val BuildVersionInfoType = deriveObjectType[Unit, BuildInfo]()
  implicit val DeveloperVersionInProcessInfoType = deriveObjectType[Unit, DeveloperVersionInProcessInfo]()
  implicit val DeveloperVersionInfoType = deriveObjectType[Unit, DeveloperVersionInfo]()
  implicit val InstallInfoType = deriveObjectType[Unit, InstallInfo]()
  implicit val ClientVersionInfoType = deriveObjectType[Unit, ClientVersionInfo]()
  implicit val VersionsInfoType = deriveObjectType[Unit, DeveloperVersionsInfo]()
  implicit val InstalledDesiredVersionsType = deriveObjectType[Unit, InstalledDesiredVersions]()
  implicit val ClientInfoType = deriveObjectType[Unit, DistributionConsumerInfo]()
  implicit val UserInfoType = deriveObjectType[Unit, UserInfo]()
  implicit val UpdateErrorType = deriveObjectType[Unit, UpdateError]()
  implicit val ServiceStateType = deriveObjectType[Unit, ServiceState]()
  implicit val DirectoryServiceStateType = deriveObjectType[Unit, DirectoryServiceState]()
  implicit val InstanceServiceStateType = deriveObjectType[Unit, InstanceServiceState]()
  implicit val DistributionServiceStateType = deriveObjectType[Unit, DistributionServiceState]()
  implicit val LogLineType = deriveObjectType[Unit, LogLine]()
  implicit val ServiceLogLineType = deriveObjectType[Unit, ServiceLogLine]()
  implicit val SequencedServiceLogLineType = deriveObjectType[Unit, SequencedServiceLogLine]()
  implicit val FaultInfoType = deriveObjectType[Unit, FaultInfo]()
  implicit val ServiceFaultReportType = deriveObjectType[Unit, ServiceFaultReport]()
  implicit val DistributionFaultReportType = deriveObjectType[Unit, DistributionFaultReport]()
  implicit val ProviderInfoType = deriveObjectType[Unit, DistributionProviderInfo]()
  implicit val ConsumerInfoType = deriveObjectType[Unit, DistributionConsumerInfo]()

  implicit val DeveloperVersionInputType = deriveInputObjectType[DeveloperVersion](InputObjectTypeName("DeveloperVersionInput"))
  implicit val ClientVersionInputType = deriveInputObjectType[ClientVersion](InputObjectTypeName("ClientVersionInput"))
  implicit val DeveloperDistributionVersionInputType = deriveInputObjectType[DeveloperDistributionVersion](InputObjectTypeName("DeveloperDistributionVersionInput"))
  implicit val ClientDistributionVersionInputType = deriveInputObjectType[ClientDistributionVersion](InputObjectTypeName("ClientDistributionVersionInput"))
  implicit val GitConfigInputType = deriveInputObjectType[GitConfig](InputObjectTypeName("GitConfigInput"))
  implicit val SourceConfigInputType = deriveInputObjectType[SourceConfig](InputObjectTypeName("SourceConfigInput"))
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