package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class TestSignature(profileName: ProfileName, clientName: ClientName, date: Date)
case class TestedVersion(profileName: ProfileName, serviceName: ServiceName, version: BuildVersion)

object TestedVersion extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val testSignatureJson = jsonFormat3(TestSignature.apply)
  implicit val testedVersionsJson = jsonFormat3(TestedVersion.apply)
}