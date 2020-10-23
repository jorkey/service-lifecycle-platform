package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class ServiceVersion(serviceName: ServiceName, buildVersion: BuildVersion)

object ServiceVersion extends DefaultJsonProtocol {
  implicit val serviceVersionJson = jsonFormat2(ServiceVersion.apply)
}

case class DesiredVersions(versions: Seq[ServiceVersion]) {
  def toMap = DesiredVersionsMap.fromList(this)

  def get(serviceName: ServiceName) =
    versions.find(_.serviceName == serviceName).map(_.buildVersion)
}

object DesiredVersions extends DefaultJsonProtocol {
  implicit val desiredVersionsJson = jsonFormat1(DesiredVersions.apply)

  def fromMap(versions: Map[ServiceName, BuildVersion]): DesiredVersions =
    DesiredVersionsMap(versions).toList
}

case class DesiredVersionsMap(versions: Map[ServiceName, BuildVersion]) {
  def toList = DesiredVersions(versions.map(entry => ServiceVersion(entry._1, entry._2)).toSeq)
}

object DesiredVersionsMap extends DefaultJsonProtocol {
  def fromList(versions: DesiredVersions): DesiredVersionsMap = {
    DesiredVersionsMap(versions.versions.foldLeft(Map.empty[ServiceName, BuildVersion])((map, version) =>
      map + (version.serviceName -> version.buildVersion)))
  }
}

case class ClientDesiredVersions(clientName: ClientName, desiredVersions: DesiredVersions)

object ClientDesiredVersions extends DefaultJsonProtocol {
  implicit val clientDesiredVersionsJson = jsonFormat2(ClientDesiredVersions.apply)
}