package com.vyulabs.update.version

import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class ClientVersion(developerVersion: DeveloperVersion, localBuild: Option[Int] = None) {
  def next() = {
    val v = localBuild match {
      case Some(localBuild) =>
        localBuild + 1
      case None =>
        1
    }
    ClientVersion(developerVersion, Some(v))
  }

  override def toString: String = {
    var version = developerVersion.toString
    for (localBuild <- localBuild) {
      version += ("_" + localBuild)
    }
    version
  }
}

object ClientVersion {
  implicit object ClientVersionJsonFormat extends RootJsonFormat[ClientVersion] {
    def write(value: ClientVersion) = JsString(value.toString)
    def read(value: JsValue) = ClientVersion.parse(value.asInstanceOf[JsString].value)
  }

  def parse(version: String): ClientVersion = {
    val index = version.indexOf('_')
    val developerVersion = DeveloperVersion.parse(if (index != -1) version.substring(0, index) else version)
    val localBuild = if (index != -1) Some(version.substring(index + 1).toInt) else None
    new ClientVersion(developerVersion, localBuild)
  }

  val ordering: Ordering[ClientVersion] = Ordering.fromLessThan[ClientVersion]((version1, version2) => {
    if (version1.developerVersion != version2.developerVersion) {
      DeveloperVersion.isLessThan(version1.developerVersion.build, version2.developerVersion.build)
    } else {
      version1.localBuild.getOrElse(0) < version2.localBuild.getOrElse(0)
    }
  })
}