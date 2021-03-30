package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{DistributionName, ConsumerProfile, ServiceName}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import spray.json.DefaultJsonProtocol

case class DeveloperDesiredVersion(serviceName: ServiceName, version: DeveloperDistributionVersion)

object DeveloperDesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersion.apply)
}

case class DeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion])

object DeveloperDesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DeveloperDesiredVersions.apply)

  def toMap(versions: Seq[DeveloperDesiredVersion]): Map[ServiceName, DeveloperDistributionVersion] = {
    versions.foldLeft(Map.empty[ServiceName, DeveloperDistributionVersion])((map, version) => map + (version.serviceName -> version.version))
  }

  def fromMap(versions: Map[ServiceName, DeveloperDistributionVersion]): Seq[DeveloperDesiredVersion] = {
    versions.foldLeft(Seq.empty[DeveloperDesiredVersion])((seq, e)=> seq :+ DeveloperDesiredVersion(e._1, e._2)).sortBy(_.serviceName)
  }
}

case class TestSignature(distributionName: DistributionName, date: Date)
case class TestedDesiredVersions(consumerProfile: ConsumerProfile, versions: Seq[DeveloperDesiredVersion], signatures: Seq[TestSignature])

case class DeveloperDesiredVersionDelta(serviceName: ServiceName, version: Option[DeveloperDistributionVersion])

object DeveloperDesiredVersionDelta extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersionDelta.apply)
}
