package com.vyulabs.update.info

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class DesiredVersion(serviceName: ServiceName, buildVersion: BuildVersion)

object DesiredVersion extends DefaultJsonProtocol {
  implicit val desiredVersionJson = jsonFormat2(DesiredVersion.apply)

  def toMap(versions: Seq[DesiredVersion]): Map[ServiceName, BuildVersion] = {
    versions.foldLeft(Map.empty[ServiceName, BuildVersion])((map, version) => map + (version.serviceName -> version.buildVersion))
  }
}

case class OptionDesiredVersion(serviceName: ServiceName, buildVersion: Option[BuildVersion])

object OptionDesiredVersion extends DefaultJsonProtocol {
  implicit val optionDesiredVersionJson = jsonFormat2(OptionDesiredVersion.apply)

  def fromMap(versions: Map[ServiceName, Option[BuildVersion]]): Seq[OptionDesiredVersion] = {
    versions.map(entry => OptionDesiredVersion(entry._1, entry._2)).toSeq
  }
}

case class ClientDesiredVersion(clientName: ClientName, serviceName: ServiceName, buildVersion: BuildVersion)

object ClientDesiredVersion extends DefaultJsonProtocol {
  implicit val clientDesiredVersionJson = jsonFormat3(ClientDesiredVersion.apply)
}