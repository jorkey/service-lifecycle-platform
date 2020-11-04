package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ProfileName}
import spray.json.DefaultJsonProtocol

case class TestSignature(clientName: ClientName, date: Date)

case class TestedDesiredVersions(profileName: ProfileName, versions: Seq[DesiredVersion], signatures: Seq[TestSignature])

object TestedDesiredVersions extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._

  implicit val testSignatureJson = jsonFormat2(TestSignature.apply)
  implicit val testedVersionsJson = jsonFormat3(TestedDesiredVersions.apply)
}