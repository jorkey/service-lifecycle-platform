package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{AccountId, ServiceId, TaskId}
import com.vyulabs.update.common.info.BuildState.BuildState
import com.vyulabs.update.common.utils.JsonFormats._
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

import java.util.Date

object BuildState extends Enumeration {
  type BuildState = Value
  val InProcess, Success, Failure = Value

  implicit object BuildStateJsonFormat extends RootJsonFormat[BuildState] {
    def write(value: BuildState) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}

case class BuildDeveloperServiceState(service: ServiceId, author: AccountId,
                                      version: DeveloperDistributionVersion, comment: String,
                                      taskId: TaskId, state: BuildState)

object BuildDeveloperServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat6(BuildDeveloperServiceState.apply)
}

case class TimedBuildDeveloperServiceState(time: Date, service: ServiceId, author: AccountId, version: DeveloperDistributionVersion, taskId: TaskId, state: BuildState)

object TimedBuildDeveloperServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat6(TimedBuildDeveloperServiceState.apply)
}

case class BuildClientServiceState(service: ServiceId, author: AccountId, version: ClientDistributionVersion, taskId: TaskId, state: BuildState)

object BuildClientServiceState extends DefaultJsonProtocol {
  implicit val buildClientServiceStateJson = jsonFormat5(BuildClientServiceState.apply)
}

case class TimedBuildClientServiceState(time: Date, service: ServiceId, author: AccountId, version: ClientDistributionVersion, taskId: TaskId, state: BuildState)

object TimedBuildClientServiceState extends DefaultJsonProtocol {
  implicit val buildClientServiceStateJson = jsonFormat6(TimedBuildClientServiceState.apply)
}
