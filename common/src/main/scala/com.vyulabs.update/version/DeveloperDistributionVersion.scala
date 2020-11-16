package com.vyulabs.update.version

import com.vyulabs.update.common.Common.DistributionName
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DeveloperDistributionVersion(distributionName: DistributionName, version: DeveloperVersion) {
  def isEmpty() = version.isEmpty()

  override def toString: String = {
    distributionName + "-" + version.toString
  }
}

object DeveloperDistributionVersion {
  implicit object BuildVersionJsonFormat extends RootJsonFormat[DeveloperDistributionVersion] {
    def write(value: DeveloperDistributionVersion) = JsString(value.toString)
    def read(value: JsValue) = DeveloperDistributionVersion.parse(value.asInstanceOf[JsString].value)
  }

  def parse(version: String): DeveloperDistributionVersion = {
    val index = version.indexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new DeveloperDistributionVersion(distributionName, DeveloperVersion.parse(body))
  }
}
