package com.vyulabs.update.common.info

import com.vyulabs.update.common.common.Common.{ServiceId, ServicesProfileId}
import spray.json.DefaultJsonProtocol

case class ServicesProfile(profile: ServicesProfileId, services: Seq[ServiceId])

object ServicesProfile extends DefaultJsonProtocol {
  implicit val profileJson = jsonFormat2(ServicesProfile.apply)
}
