package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.version.ClientDistributionVersion
import spray.json.DefaultJsonProtocol

case class ClientDesiredVersion(service: ServiceName, version: ClientDistributionVersion)

object ClientDesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(ClientDesiredVersion.apply)
}

case class ClientDesiredVersions(versions: Seq[ClientDesiredVersion])

object ClientDesiredVersions extends DefaultJsonProtocol {
  implicit val clientDesiredVersionsJson = jsonFormat1(ClientDesiredVersions.apply)

  def toMap(versions: Seq[ClientDesiredVersion]): Map[ServiceName, ClientDistributionVersion] = {
    versions.foldLeft(Map.empty[ServiceName, ClientDistributionVersion])((map, version) => map + (version.service -> version.version))
  }

  def fromMap(versions: Map[ServiceName, ClientDistributionVersion]): Seq[ClientDesiredVersion] = {
    versions.foldLeft(Seq.empty[ClientDesiredVersion])((seq, e)=> seq :+ ClientDesiredVersion(e._1, e._2)).sortBy(_.service)
  }
}

case class ClientDesiredVersionDelta(service: ServiceName, version: Option[ClientDistributionVersion])

object ClientDesiredVersionDelta extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(ClientDesiredVersionDelta.apply)
}

