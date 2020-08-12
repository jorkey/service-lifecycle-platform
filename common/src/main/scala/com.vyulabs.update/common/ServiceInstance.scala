package com.vyulabs.update.common

import Common._

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 04.03.19.
  * Copyright FanDate, Inc.
  */

case class ServiceInstanceName(serviceName: ServiceName, serviceProfile: ServiceProfile) {
  override def toString: String = {
    if (serviceProfile != CommonProfile) {
      serviceName + "-" + serviceProfile
    } else {
      serviceName
    }
  }
}

object ServiceInstanceName {
  def apply(serviceName: ServiceName): ServiceInstanceName = {
    ServiceInstanceName(serviceName, CommonProfile)
  }

  def apply(serviceName: ServiceName, serviceProfile: ServiceProfile): ServiceInstanceName = {
    new ServiceInstanceName(serviceName, serviceProfile)
  }

  def parse(name: String): ServiceInstanceName = {
    val fields = name.split("-")
    if (fields.size == 1) {
      ServiceInstanceName(fields(0), CommonProfile)
    } else if (fields.size == 2) {
      ServiceInstanceName(fields(0), fields(1))
    } else {
      sys.error(s"Invalid service instance name ${name}")
    }
  }
}

object ServiceInstanceJson extends DefaultJsonProtocol {
  implicit object ServiceInstanceNameJsonFormat extends RootJsonFormat[ServiceInstanceName] {
    def write(value: ServiceInstanceName) = JsString(value.toString)
    def read(value: JsValue) = ServiceInstanceName.parse(value.asInstanceOf[JsString].value)
  }
}