package com.vyulabs.update.version

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.02.19.
  * Copyright FanDate, Inc.
  */
class ClientVersionTest extends FlatSpec with Matchers {
  behavior of "ClientVersion"

  it should "parse version" in {
    val version = ClientVersion.parse("1.2")
    assertResult(ClientVersion(DeveloperVersion(1, 2)))(version)
    val localVersion = ClientVersion.parse("3.2_1")
    assertResult(ClientVersion(DeveloperVersion(3, 2), Some(1)))(localVersion)
  }

  it should "serialize version" in {
    val version = ClientVersion.parse("2.1")
    assertResult("2.1")(version.toString)
    val localVersion = ClientVersion.parse("3.2_1")
  }

  it should "sort versions" in {
    val version1 = ClientVersion.parse("5.1")
    val version2 = ClientVersion.parse("2.3")
    val version3 = ClientVersion.parse("1.1")
    val version4 = ClientVersion.parse("1.1.1")
    val version5 = ClientVersion.parse("2.3_2")
    val version6 = ClientVersion.parse("2.3_1")

    val sorted = Seq(version1, version2, version3, version4, version5, version6).sorted(ClientVersion.ordering)
    assertResult(Seq(version3, version4, version2, version6, version5, version1))(sorted)
  }

  it should "throw exception when can't parse version" in {
    var throwed = false
    try {
      val version1 = ClientVersion.parse("e51")
    } catch {
      case ex: Exception =>
        throwed = true
    }
    assertResult(true)(throwed)
  }
}
