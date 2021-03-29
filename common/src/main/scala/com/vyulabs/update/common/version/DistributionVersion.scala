package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionName
import spray.json.DefaultJsonProtocol._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DistributionVersion(distributionName: DistributionName, build: Version) {
  def isEmpty() = build.isEmpty()

  def next() = DistributionVersion(distributionName, build.next())

  override def toString: String = {
    distributionName + "-" + build.toString
  }
}

object DistributionVersion {
  implicit val distributionVersionJson = jsonFormat2(DistributionVersion.apply)

  def parse(version: String): DistributionVersion = {
    val index = version.lastIndexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new DistributionVersion(distributionName, Version.parse(body))
  }

  val ordering: Ordering[DistributionVersion] = Ordering.fromLessThan[DistributionVersion]((version1, version2) => {
    if (version1.distributionName != version2.distributionName) {
      version1.distributionName.compareTo(version2.distributionName) < 0
    } else {
      Version.ordering.lt(version1.build, version2.build)
    }
  })
}
