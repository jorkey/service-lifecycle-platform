package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{CommonServiceProfile, ServiceId, ServiceInstanceProfile}
import spray.json.{JsString, JsValue, RootJsonFormat}

case class ProfiledServiceName(name: ServiceId, profile: ServiceInstanceProfile) {
  override def toString: String = {
    if (profile != CommonServiceProfile) {
      name + "-" + profile
    } else {
      name
    }
  }
}

object ProfiledServiceName {
  implicit object ServiceInstanceNameJsonFormat extends RootJsonFormat[ProfiledServiceName] {
    def write(value: ProfiledServiceName) = JsString(value.toString)
    def read(value: JsValue) = ProfiledServiceName.parse(value.asInstanceOf[JsString].value)
  }

  def apply(service: ServiceId): ProfiledServiceName = {
    ProfiledServiceName(service, CommonServiceProfile)
  }

  def apply(service: ServiceId, serviceProfile: ServiceInstanceProfile): ProfiledServiceName = {
    new ProfiledServiceName(service, serviceProfile)
  }

  def parse(name: String): ProfiledServiceName = {
    val fields = name.split("-")
    if (fields.size == 1) {
      ProfiledServiceName(fields(0), CommonServiceProfile)
    } else if (fields.size == 2) {
      ProfiledServiceName(fields(0), fields(1))
    } else {
      sys.error(s"Invalid service instance name ${name}")
    }
  }
}

