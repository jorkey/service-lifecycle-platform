package com.vyulabs.update.info

import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import spray.json.DefaultJsonProtocol

case class ServicesVersions(servicesVersions: Map[ServiceName, ClientDistributionVersion])

object ServicesVersions extends DefaultJsonProtocol {
  import com.vyulabs.update.version.ClientDistributionVersion._

  implicit val servicesVersionsJson = jsonFormat1(ServicesVersions.apply)
}