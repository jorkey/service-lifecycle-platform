package com.vyulabs.update.version

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.02.19.
  * Copyright FanDate, Inc.
  */
class BuildVersionTest extends FlatSpec with Matchers {
  behavior of "BuildVersion"

  val clientName = "client"

  it should "parse version" in {
    val version = BuildVersion.parse("client-1.2")
    assertResult(BuildVersion(clientName, 1, 2))(version)
  }

  it should "serialize version" in {
    val version = BuildVersion.parse("client-2.1")
    assertResult("client-2.1")(version.toString)
  }

  it should "sort versions" in {
    val version1 = BuildVersion.parse("client-5.1")
    val version2 = BuildVersion.parse("client-2.3")
    val version3 = BuildVersion.parse("client-1.1")
    val version4 = BuildVersion.parse("client-1.1")
    val version5 = BuildVersion.parse("client-2.3")

    val sorted = Seq(version1, version2, version3, version4, version5).sorted(BuildVersion.ordering)
    assertResult(Seq(version3, version4, version2, version5, version1))(sorted)
  }

  it should "throw exception when can't parse version" in {
    var throwed = false
    try {
      val version1 = BuildVersion.parse("e51")
    } catch {
      case ex: Exception =>
        throwed = true
    }
    assertResult(true)(throwed)
  }
}
