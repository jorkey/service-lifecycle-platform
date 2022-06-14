package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{AccountId, ServiceId, TaskId}
import com.vyulabs.update.common.info.BuildStatus.BuildStatus
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

import java.util.Date

object BuildStatus extends Enumeration {
  type BuildStatus = Value
  val InProcess, Success, Failure = Value

  implicit object BuildStatusJsonFormat extends RootJsonFormat[BuildStatus] {
    def write(value: BuildStatus) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}

case class BuildDeveloperServiceState(service: ServiceId, author: AccountId,
                                      version: DeveloperVersion, comment: String,
                                      task: TaskId, status: BuildStatus)

object BuildDeveloperServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat6(BuildDeveloperServiceState.apply)
}

case class ServerBuildDeveloperServiceState(service: ServiceId, author: AccountId,
                                            version: DeveloperVersion, comment: String,
                                            task: TaskId, status: String)

case class TimedBuildDeveloperServiceState(time: Date, service: ServiceId, author: AccountId, version: DeveloperVersion,
                                           comment: String, task: TaskId, status: BuildStatus)

object TimedBuildDeveloperServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat7(TimedBuildDeveloperServiceState.apply)
}

case class BuildClientServiceState(service: ServiceId, author: AccountId, version: DeveloperDistributionVersion,
                                   task: TaskId, status: BuildStatus)

object BuildClientServiceState extends DefaultJsonProtocol {
  implicit val buildClientServiceStateJson = jsonFormat5(BuildClientServiceState.apply)
}

case class ServerBuildClientServiceState(service: ServiceId, author: AccountId,
                                         version: DeveloperDistributionVersion, task: TaskId, status: String)

case class TimedBuildClientServiceState(time: Date, service: ServiceId, author: AccountId, version: DeveloperDistributionVersion,
                                        task: TaskId, status: BuildStatus)

object TimedBuildClientServiceState extends DefaultJsonProtocol {
  implicit val buildClientServiceStateJson = jsonFormat6(TimedBuildClientServiceState.apply)
}
