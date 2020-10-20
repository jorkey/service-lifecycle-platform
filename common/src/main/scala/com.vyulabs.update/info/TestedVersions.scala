package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ProfileName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.DefaultJsonProtocol

case class TestSignature(clientName: ClientName, date: Date)

case class TestedVersions(profileName: ProfileName, versions: Map[ServiceName, BuildVersion], signatures: Seq[TestSignature])

object TestedVersions extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val testSignatureJson = jsonFormat2(TestSignature.apply)
  implicit val testedVersionsJson = jsonFormat3(TestedVersions.apply)
}