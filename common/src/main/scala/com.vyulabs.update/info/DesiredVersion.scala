package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class DesiredVersion(serviceName: ServiceName, buildVersion: BuildVersion)

object DesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DesiredVersion.apply)
}

case class DesiredVersions(versions: Seq[DesiredVersion])

object DesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DesiredVersions.apply)

  def toMap(versions: Seq[DesiredVersion]): Map[ServiceName, BuildVersion] = {
    versions.foldLeft(Map.empty[ServiceName, BuildVersion])((map, version) => map + (version.serviceName -> version.buildVersion))
  }

  def fromMap(versions: Map[ServiceName, BuildVersion]): Seq[DesiredVersion] = {
    versions.foldLeft(Seq.empty[DesiredVersion])((seq, e)=> seq :+ DesiredVersion(e._1, e._2)).sortBy(_.serviceName)
  }
}

