package com.vyulabs.update.common.version

import com.vyulabs.update.common.common.Common.DistributionName
import spray.json.DefaultJsonProtocol._


/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientDistributionVersion(distributionName: DistributionName, developerBuild: Seq[Int], clientBuild: Int) {
  def clientVersion = ClientVersion(developerBuild, clientBuild)

  def original = DeveloperDistributionVersion(distributionName, developerBuild)

  def next = ClientDistributionVersion(distributionName, developerBuild, clientBuild + 1)

  override def toString: String = {
    distributionName + "-" + clientVersion.toString
  }
}

object ClientDistributionVersion {
  implicit val clientDistributionVersionJson = jsonFormat3(ClientDistributionVersion.apply)

  def from(distributionName: DistributionName, version: ClientVersion): ClientDistributionVersion = {
    ClientDistributionVersion(distributionName, version.developerBuild, version.clientBuild)
  }

  def from(distributionName: DistributionName, version: DeveloperVersion, clientBuild: Int): ClientDistributionVersion =
    ClientDistributionVersion(distributionName, version.build, clientBuild)

  def from(version: DeveloperDistributionVersion, clientBuild: Int): ClientDistributionVersion =
    ClientDistributionVersion(version.distributionName, version.build, clientBuild)

  def parse(version: String): ClientDistributionVersion = {
    val index = version.lastIndexOf('-')
    if (index == -1) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val distributionName = version.substring(0, index)
    val body = if (index != -1) version.substring(index + 1) else version
    ClientDistributionVersion.from(distributionName, ClientVersion.parse(body))
  }

  val ordering: Ordering[ClientDistributionVersion] = Ordering.fromLessThan[ClientDistributionVersion]((version1, version2) => {
    if (version1.distributionName != version2.distributionName) {
      version1.distributionName.compareTo(version2.distributionName) < 0
    } else {
      ClientVersion.ordering.lt(version1.clientVersion, version2.clientVersion)
    }
  })
}


