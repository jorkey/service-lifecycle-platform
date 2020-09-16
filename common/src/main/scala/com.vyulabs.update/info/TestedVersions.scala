package com.vyulabs.update.info

import java.util.Date

import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.version.BuildVersion
import spray.json.{DefaultJsonProtocol}

case class TestedVersions(testedVersions: Map[ServiceName, BuildVersion], testSignatures: Seq[TestSignature])
case class TestSignature(clientName: ClientName, date: Date)

object TestedVersions extends DefaultJsonProtocol {
  import com.vyulabs.update.utils.Utils.DateJson._
  import com.vyulabs.update.version.BuildVersion._

  implicit val testSignatureJson = jsonFormat2(TestSignature.apply)
  implicit val testedVersionsJson = jsonFormat2(TestedVersions.apply)
}