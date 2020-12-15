package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionName
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientDistributionVersion(distributionName: DistributionName, version: ClientVersion) {
  def original() = DeveloperDistributionVersion(distributionName, version.developerVersion)

  def next() = ClientDistributionVersion(distributionName, version.next())

  override def toString: String = {
    distributionName + "-" + version.toString
  }
}

object ClientDistributionVersion {
  implicit object BuildVersionJsonFormat extends RootJsonFormat[ClientDistributionVersion] {
    def write(value: ClientDistributionVersion) = JsString(value.toString)
    def read(value: JsValue) = ClientDistributionVersion.parse(value.asInstanceOf[JsString].value)
  }

  def apply(version: DeveloperDistributionVersion): ClientDistributionVersion =
    ClientDistributionVersion(version.distributionName, ClientVersion(version.version))

  def parse(version: String): ClientDistributionVersion = {
    val index = version.indexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new ClientDistributionVersion(distributionName, ClientVersion.parse(body))
  }

  val ordering: Ordering[ClientDistributionVersion] = Ordering.fromLessThan[ClientDistributionVersion]((version1, version2) => {
    if (version1.distributionName != version2.distributionName) {
      version1.distributionName.compareTo(version2.distributionName) < 0
    } else {
      ClientVersion.ordering.lt(version1.version, version2.version)
    }
  })
}


