package com.vyulabs.update.common.version

import spray.json.DefaultJsonProtocol.{jsonFormat2, _}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientVersion(developerBuild: Seq[Int], clientBuild: Int) {
  def isEmpty() = developerBuild.isEmpty

  def next() = {
    ClientVersion(developerBuild, clientBuild+1)
  }

  override def toString: String = {
    if (clientBuild != 0) {
      Build.toString(developerBuild) + "_" + clientBuild
    } else {
      Build.toString(developerBuild)
    }
  }
}

object ClientVersion {
  implicit val clientVersionJson = jsonFormat2(ClientVersion.apply)

  def parse(version: String): ClientVersion = {
    val index = version.lastIndexOf('_')
    val developerVersion = Build.parse(if (index != -1) version.substring(0, index) else version)
    val clientBuild = if (index != -1) version.substring(index + 1).toInt else 0
    new ClientVersion(developerVersion, clientBuild)
  }

  val ordering: Ordering[ClientVersion] = Ordering.fromLessThan[ClientVersion]((version1, version2) => {
    if (version1.developerBuild != version2.developerBuild) {
      Build.isLessThan(version1.developerBuild, version2.developerBuild)
    } else {
      version1.clientBuild < version2.clientBuild
    }
  })
}