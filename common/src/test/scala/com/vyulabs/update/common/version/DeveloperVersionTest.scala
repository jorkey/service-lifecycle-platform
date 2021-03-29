package com.vyulabs.update.common.version

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.02.19.
  * Copyright FanDate, Inc.
  */
class DeveloperVersionTest extends FlatSpec with Matchers {
  behavior of "DeveloperVersion"

  it should "parse version" in {
    val version = Version.parse("1.2")
    assertResult(Version(Seq(1, 2)))(version)
  }

  it should "serialize version" in {
    val version = Version.parse("2.1")
    assertResult("2.1")(version.toString)
  }

  it should "sort versions" in {
    val version1 = Version.parse("5.1")
    val version2 = Version.parse("2.3")
    val version3 = Version.parse("1.1")
    val version4 = Version.parse("1.1.1")

    val sorted = Seq(version1, version2, version3, version4).sorted(Version.ordering)
    assertResult(Seq(version3, version4, version2, version1))(sorted)
  }

  it should "throw exception when can't parse version" in {
    var throwed = false
    try {
      val version1 = Version.parse("e51")
    } catch {
      case ex: Exception =>
        throwed = true
    }
    assertResult(true)(throwed)
  }
}
