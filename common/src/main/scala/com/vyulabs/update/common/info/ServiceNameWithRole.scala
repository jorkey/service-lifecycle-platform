package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServiceId, ServiceRole}
import spray.json.{JsString, JsValue, RootJsonFormat}

case class ServiceNameWithRole(name: ServiceId, role: Option[ServiceRole]) {
  override def toString: String = {
    role match {
      case Some(role) =>
        name + "-" + role
      case None =>
        name
    }
  }
}

object ServiceNameWithRole {
  implicit object ServiceInstanceNameJsonFormat extends RootJsonFormat[ServiceNameWithRole] {
    def write(value: ServiceNameWithRole) = JsString(value.toString)
    def read(value: JsValue) = ServiceNameWithRole.parse(value.asInstanceOf[JsString].value)
  }

  def apply(service: ServiceId, role: Option[ServiceRole] = None): ServiceNameWithRole = {
    new ServiceNameWithRole(service, role)
  }

  def parse(name: String): ServiceNameWithRole = {
    val fields = name.split("-")
    if (fields.size == 1) {
      ServiceNameWithRole(fields(0))
    } else if (fields.size == 2) {
      ServiceNameWithRole(fields(0), Some(fields(1)))
    } else {
      sys.error(s"Invalid service instance name ${name}")
    }
  }
}

