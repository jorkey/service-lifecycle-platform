package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.{DefaultJsonProtocol}

case class DesiredVersions(desiredVersions: Map[ServiceName, BuildVersion], testSignatures: Seq[TestSignature] = Seq.empty)
case class TestSignature(clientName: ClientName, date: Date)

object DesiredVersionsJson extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersionJson._

  implicit val testSignatureJson = jsonFormat2(TestSignature.apply)
  implicit val desiredVersionsJson = jsonFormat2(DesiredVersions.apply)
}