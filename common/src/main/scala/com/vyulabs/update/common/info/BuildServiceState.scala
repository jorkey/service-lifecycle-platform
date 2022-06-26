package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{AccountId, ServiceId, TaskId}
import com.vyulabs.update.common.info.BuildStatus.BuildStatus
import com.vyulabs.update.common.info.BuildTarget.BuildTarget
import com.vyulabs.update.common.utils.JsonFormats._
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

import java.util.Date

object BuildTarget extends Enumeration {
  type BuildTarget = Value
  val DeveloperVersion, ClientVersion = Value

  implicit object BuildTargetJsonFormat extends RootJsonFormat[BuildTarget] {
    def write(value: BuildTarget) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}

object BuildStatus extends Enumeration {
  type BuildStatus = Value
  val InProcess, Success, Failure = Value

  implicit object BuildStatusJsonFormat extends RootJsonFormat[BuildStatus] {
    def write(value: BuildStatus) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}

case class BuildServiceState(service: ServiceId, target: BuildTarget,
                             author: AccountId, version: String, comment: String,
                             task: TaskId, status: BuildStatus)

object BuildServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat7(BuildServiceState.apply)
}

case class ServerBuildServiceState(service: ServiceId, target: String,
                                   author: AccountId, version: String, comment: String,
                                   task: TaskId, status: String)

case class TimedBuildServiceState(time: Date, service: ServiceId, target: BuildTarget,
                                  author: AccountId, version: String,
                                  comment: String, task: TaskId, status: BuildStatus)

object TimedBuildServiceState extends DefaultJsonProtocol {
  implicit val buildDeveloperServiceStateJson = jsonFormat8(TimedBuildServiceState.apply)
}
