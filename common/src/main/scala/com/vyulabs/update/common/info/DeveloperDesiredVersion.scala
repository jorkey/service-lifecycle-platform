package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{DistributionId, ServiceId, ServicesProfileId}
import com.vyulabs.update.common.version.DeveloperDistributionVersion
import spray.json.DefaultJsonProtocol
import com.vyulabs.update.common.utils.JsonFormats.DateJsonFormat

import java.util.Date

case class DeveloperDesiredVersion(service: ServiceId, version: DeveloperDistributionVersion) {
  override def toString(): String = {
    s"${service} -> ${version.toString}"
  }
}

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

case class TimedDeveloperDesiredVersions(time: Date, versions: Seq[DeveloperDesiredVersion])

object TimedDeveloperDesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsRecordJson = jsonFormat2(TimedDeveloperDesiredVersions.apply)
}

case class TestedVersions(profile: ServicesProfileId, consumerDistribution: DistributionId,
                          versions: Seq[DeveloperDesiredVersion], time: Date)

case class DeveloperDesiredVersionDelta(service: ServiceId, version: Option[DeveloperDistributionVersion])

object DeveloperDesiredVersionDelta extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DeveloperDesiredVersionDelta.apply)
}
