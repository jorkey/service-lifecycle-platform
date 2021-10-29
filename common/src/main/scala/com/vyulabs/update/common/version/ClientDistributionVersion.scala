package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionId
import spray.json.DefaultJsonProtocol._


/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientDistributionVersion(distribution: DistributionId, developerBuild: Seq[Int], clientBuild: Int) {
  def clientVersion = ClientVersion(developerBuild, clientBuild)

  def original = DeveloperDistributionVersion(distribution, developerBuild)

  def next = ClientDistributionVersion(distribution, developerBuild, clientBuild + 1)

  override def toString: String = {
    distribution + "-" + clientVersion.toString
  }
}

object ClientDistributionVersion {
  implicit val clientDistributionVersionJson = jsonFormat3(ClientDistributionVersion.apply)

  def from(distribution: DistributionId, version: ClientVersion): ClientDistributionVersion = {
    ClientDistributionVersion(distribution, version.developerBuild, version.clientBuild)
  }

  def from(distribution: DistributionId, version: DeveloperVersion, clientBuild: Int): ClientDistributionVersion =
    ClientDistributionVersion(distribution, version.build, clientBuild)

  def from(version: DeveloperDistributionVersion, clientBuild: Int): ClientDistributionVersion =
    ClientDistributionVersion(version.distribution, version.build, clientBuild)

  def parse(version: String): ClientDistributionVersion = {
    val index = version.lastIndexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distribution = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    ClientDistributionVersion.from(distribution, ClientVersion.parse(body))
  }

  val ordering: Ordering[ClientDistributionVersion] = Ordering.fromLessThan[ClientDistributionVersion]((version1, version2) => {
    if (version1.distribution != version2.distribution) {
      version1.distribution.compareTo(version2.distribution) < 0
    } else {
      ClientVersion.ordering.lt(version1.clientVersion, version2.clientVersion)
    }
  })
}


