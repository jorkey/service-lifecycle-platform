package com.vyulabs.update.common.info

import spray.json.{JsString, JsValue, RootJsonFormat}

object AccountRole extends Enumeration {
  type AccountRole = Value
  val None, Developer, Administrator, DistributionConsumer, Builder, Updater = Value

  implicit object AccountRoleJsonFormat extends RootJsonFormat[AccountRole] {
    def write(value: AccountRole) = JsString(value.toString)
    def read(value: JsValue) = withName(value.asInstanceOf[JsString].value)
  }
}
