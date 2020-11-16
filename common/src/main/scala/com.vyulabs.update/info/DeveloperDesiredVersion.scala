package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.version.DeveloperDistributionVersion
import spray.json.DefaultJsonProtocol

case class DeveloperDesiredVersion(serviceName: ServiceName, buildVersion: DeveloperDistributionVersion)

object DeveloperDesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersion.apply)
}

case class DeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion])

object DeveloperDesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DeveloperDesiredVersions.apply)

  def toMap(versions: Seq[DeveloperDesiredVersion]): Map[ServiceName, DeveloperDistributionVersion] = {
    versions.foldLeft(Map.empty[ServiceName, DeveloperDistributionVersion])((map, version) => map + (version.serviceName -> version.buildVersion))
  }

  def fromMap(versions: Map[ServiceName, DeveloperDistributionVersion]): Seq[DeveloperDesiredVersion] = {
    versions.foldLeft(Seq.empty[DeveloperDesiredVersion])((seq, e)=> seq :+ DeveloperDesiredVersion(e._1, e._2)).sortBy(_.serviceName)
  }
}

case class TestSignature(clientName: ClientName, date: Date)
case class TestedDesiredVersions(profileName: ProfileName, versions: Seq[DeveloperDesiredVersion], signatures: Seq[TestSignature])
