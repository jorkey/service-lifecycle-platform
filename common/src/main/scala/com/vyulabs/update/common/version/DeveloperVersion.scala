package com.vyulabs.update.common.version

import spray.json.DefaultJsonProtocol._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DeveloperVersion(build: Seq[Int]) {
  def isEmpty = build.isEmpty

  def next = {
    DeveloperVersion(Build.next(build))
  }

  override def toString: String = {
    Build.toString(build)
  }
}

object DeveloperVersion {
  implicit val developerVersionJson = jsonFormat1(DeveloperVersion.apply)

  def parse(version: String): DeveloperVersion = {
    val build = Build.parse(version)
    new DeveloperVersion(build)
  }

  val ordering: Ordering[DeveloperVersion] = Ordering.fromLessThan[DeveloperVersion]((version1, version2) => {
    Build.isLessThan(version1.build, version2.build)
  })
}