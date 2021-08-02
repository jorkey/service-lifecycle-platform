package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionId
import spray.json.DefaultJsonProtocol._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DeveloperDistributionVersion(distribution: DistributionId, build: Seq[Int]) {
  def developerVersion = DeveloperVersion(build)

  def isEmpty = developerVersion.isEmpty

  def next = DeveloperDistributionVersion.from(distribution, developerVersion.next)

  override def toString: String = {
    distribution + "-" + developerVersion.toString
  }
}

object DeveloperDistributionVersion {
  implicit val distributionVersionJson = jsonFormat2(DeveloperDistributionVersion.apply)

  val ordering: Ordering[DeveloperDistributionVersion] = Ordering.fromLessThan[DeveloperDistributionVersion]((version1, version2) => {
    if (version1.distribution != version2.distribution) {
      version1.distribution.compareTo(version2.distribution) < 0
    } else {
      Build.ordering.lt(version1.build, version2.build)
    }
  })

  def from(distribution: DistributionId, version: DeveloperVersion): DeveloperDistributionVersion = {
    DeveloperDistributionVersion(distribution, version.build)
  }

  def from(version: ClientDistributionVersion): DeveloperDistributionVersion = {
    DeveloperDistributionVersion(version.distribution, version.developerBuild)
  }

  def parse(version: String): DeveloperDistributionVersion = {
    val index = version.lastIndexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distribution = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    new DeveloperDistributionVersion(distribution, Build.parse(body))
  }
}
