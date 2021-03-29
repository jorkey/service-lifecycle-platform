package com.vyulabs.update.common.version

import spray.json.DefaultJsonProtocol.{jsonFormat2, _}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientVersion(build: Version, clientBuild: Int) {
  def next() = {
    ClientVersion(build, clientBuild+1)
  }

  override def toString: String = {
    if (clientBuild != 0) {
      build.toString + "_" + clientBuild
    } else {
      build.toString
    }
  }
}

object ClientVersion {
  implicit val clientVersionJson = jsonFormat2(ClientVersion.apply)

  def parse(version: String): ClientVersion = {
    val index = version.lastIndexOf('_')
    val developerVersion = Version.parse(if (index != -1) version.substring(0, index) else version)
    val clientBuild = if (index != -1) version.substring(index + 1).toInt else 0
    new ClientVersion(developerVersion, clientBuild)
  }

  val ordering: Ordering[ClientVersion] = Ordering.fromLessThan[ClientVersion]((version1, version2) => {
    if (version1.build != version2.build) {
      Version.isLessThan(version1.build.build, version2.build.build)
    } else {
      version1.clientBuild < version2.clientBuild
    }
  })
}