package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.ClientDistributionVersion
import spray.json.DefaultJsonProtocol

case class ClientDesiredVersion(serviceName: ServiceName, version: ClientDistributionVersion)

object ClientDesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(ClientDesiredVersion.apply)
}

case class ClientDesiredVersions(versions: Seq[ClientDesiredVersion])

object ClientDesiredVersions extends DefaultJsonProtocol {
  implicit val clientDesiredVersionsJson = jsonFormat1(ClientDesiredVersions.apply)

  def toMap(versions: Seq[ClientDesiredVersion]): Map[ServiceName, ClientDistributionVersion] = {
    versions.foldLeft(Map.empty[ServiceName, ClientDistributionVersion])((map, version) => map + (version.serviceName -> version.version))
  }

  def fromMap(versions: Map[ServiceName, ClientDistributionVersion]): Seq[ClientDesiredVersion] = {
    versions.foldLeft(Seq.empty[ClientDesiredVersion])((seq, e)=> seq :+ ClientDesiredVersion(e._1, e._2)).sortBy(_.serviceName)
  }
}
