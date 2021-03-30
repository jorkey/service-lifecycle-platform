package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionName
import spray.json.DefaultJsonProtocol._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DeveloperDistributionVersion(distributionName: DistributionName, build: Seq[Int]) {
  def developerVersion = DeveloperVersion(build)

  def isEmpty = developerVersion.isEmpty

  def next = DeveloperDistributionVersion.from(distributionName, developerVersion.next)

  override def toString: String = {
    distributionName + "-" + developerVersion.toString
  }
}

object DeveloperDistributionVersion {
  implicit val distributionVersionJson = jsonFormat2(DeveloperDistributionVersion.apply)

  val ordering: Ordering[DeveloperDistributionVersion] = Ordering.fromLessThan[DeveloperDistributionVersion]((version1, version2) => {
    if (version1.distributionName != version2.distributionName) {
      version1.distributionName.compareTo(version2.distributionName) < 0
    } else {
      Build.ordering.lt(version1.build, version2.build)
    }
  })

  def from(distributionName: DistributionName, version: DeveloperVersion): DeveloperDistributionVersion = {
    DeveloperDistributionVersion(distributionName, version.build)
  }

  def parse(version: String): DeveloperDistributionVersion = {
    val index = version.lastIndexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new DeveloperDistributionVersion(distributionName, Build.parse(body))
  }
}
