package com.vyulabs.update.common.info

import java.util.Date

import com.vyulabs.update.common.common.Common.{DistributionId, ConsumerProfile, ServiceId}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import spray.json.DefaultJsonProtocol

case class DeveloperDesiredVersion(service: ServiceId, version: DeveloperDistributionVersion)

object DeveloperDesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersion.apply)
}

case class DeveloperDesiredVersions(versions: Seq[DeveloperDesiredVersion])

object DeveloperDesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DeveloperDesiredVersions.apply)

  def toMap(versions: Seq[DeveloperDesiredVersion]): Map[ServiceId, DeveloperDistributionVersion] = {
    versions.foldLeft(Map.empty[ServiceId, DeveloperDistributionVersion])((map, version) => map + (version.service -> version.version))
  }

  def fromMap(versions: Map[ServiceId, DeveloperDistributionVersion]): Seq[DeveloperDesiredVersion] = {
    versions.foldLeft(Seq.empty[DeveloperDesiredVersion])((seq, e)=> seq :+ DeveloperDesiredVersion(e._1, e._2)).sortBy(_.service)
  }
}

case class TestSignature(distribution: DistributionId, date: Date)
case class TestedDesiredVersions(consumerProfile: ConsumerProfile, versions: Seq[DeveloperDesiredVersion], signatures: Seq[TestSignature])

case class DeveloperDesiredVersionDelta(service: ServiceId, version: Option[DeveloperDistributionVersion])

object DeveloperDesiredVersionDelta extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersionDelta.apply)
}
