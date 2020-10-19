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
    val version = BuildVersion.parse("1.2")
    assertResult(BuildVersion(1, 2))(version)
    val clientVersion = BuildVersion.parse("client-1.2")
    assertResult(BuildVersion(clientName, 1, 2))(clientVersion)
    val localVersion = BuildVersion.parse("3.2_1")
    assertResult(BuildVersion(Seq(3, 2), Some(1)))(localVersion)
    val localClientVersion = BuildVersion.parse("client-3.2_1")
    assertResult(BuildVersion(clientName, Seq(3, 2), Some(1)))(localClientVersion)
  }

  it should "serialize version" in {
    val version = BuildVersion.parse("2.1")
    assertResult("2.1")(version.toString)
    val clientVersion = BuildVersion.parse("client-1.2")
    assertResult("client-1.2")(clientVersion.toString)
    val localVersion = BuildVersion.parse("3.2_1")
    assertResult("3.2_1")(localVersion.toString)
    val localClientVersion = BuildVersion.parse("client-3.2_1")
    assertResult("client-3.2_1")(localClientVersion.toString)
  }

  it should "sort versions" in {
    val version1 = BuildVersion.parse("5.1")
    val version2 = BuildVersion.parse("2.3")
    val version3 = BuildVersion.parse("1.1")
    val version4 = BuildVersion.parse("1.1.1")
    val version5 = BuildVersion.parse("2.3_2")
    val version6 = BuildVersion.parse("2.3_1")

    val sorted = Seq(version1, version2, version3, version4, version5, version6).sorted(BuildVersion.ordering)
    assertResult(Seq(version3, version4, version2, version6, version5, version1))(sorted)
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
