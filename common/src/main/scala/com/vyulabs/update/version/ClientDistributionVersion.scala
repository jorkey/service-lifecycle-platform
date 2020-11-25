package com.vyulabs.update.version

import com.vyulabs.update.common.Common.DistributionName
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientDistributionVersion(distributionName: DistributionName, version: ClientVersion) {
  def original() = DeveloperDistributionVersion(distributionName, version.developerVersion)

  override def toString: String = {
    distributionName + "-" + version.toString
  }
}

object ClientDistributionVersion {
  implicit object BuildVersionJsonFormat extends RootJsonFormat[ClientDistributionVersion] {
    def write(value: ClientDistributionVersion) = JsString(value.toString)
    def read(value: JsValue) = ClientDistributionVersion.parse(value.asInstanceOf[JsString].value)
  }

  def parse(version: String): ClientDistributionVersion = {
    val index = version.indexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new ClientDistributionVersion(distributionName, ClientVersion.parse(body))
  }
}


