package com.vyulabs.update.version

import com.vyulabs.update.common.Common
import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class BuildVersion(client: Option[Common.ClientName], build: Seq[Int], localBuild: Option[Int] = None) {
  def isEmpty(): Boolean = {
    build.foreach { v => if (v != 0) return false }
    true
  }

  def original(): BuildVersion = {
    BuildVersion(client, build)
  }

  def next() = {
    val v = build.last + 1
    new BuildVersion(client, build.dropRight(1) :+ v)
  }

  def nextLocal() = {
    val v = localBuild match {
      case Some(localBuild) =>
        localBuild + 1
      case None =>
        1
    }
    new BuildVersion(client, build, Some(v))
  }

  override def toString: String = {
    var version = ""
    for (client <- client) {
      version += (client + "-")
    }
    version += build.foldLeft("")((s, v) => if (s.isEmpty) v.toString else s + "." + v.toString)
    for (localBuild <- localBuild) {
      version += ("_" + localBuild)
    }
    version
  }
}

object BuildVersion {
  implicit object BuildVersionJsonFormat extends RootJsonFormat[BuildVersion] {
    def write(value: BuildVersion) = JsString(value.toString)
    def read(value: JsValue) = BuildVersion.parse(value.asInstanceOf[JsString].value)
  }

  def apply(build: Int*): BuildVersion = {
    new BuildVersion(None, build)
  }

  def apply(build: Seq[Int], localBuild: Option[Int]): BuildVersion = {
    new BuildVersion(None, build, localBuild)
  }

  def apply(clientName: Common.ClientName, build: Int*): BuildVersion = {
    new BuildVersion(Some(clientName), build)
  }

  def apply(clientName: Common.ClientName, build: Seq[Int], localBuild: Option[Int]): BuildVersion = {
    new BuildVersion(Some(clientName), build, localBuild)
  }

  def parse(version: String): BuildVersion = {
    val index = version.indexOf('-')
    val client = if (index != -1) Some(version.substring(0, index)) else None
    val body = if (index != -1) version.substring(index + 1) else version
    val index1 = body.indexOf('_')
    val versionBody = if (index1 != -1) body.substring(0, index1) else body
    val build = versionBody.split("\\.").map(_.toInt)
    if (build.size == 0) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    val localBuild = if (index1 != -1) Some(body.substring(index1 + 1).toInt) else None
    new BuildVersion(client, build, localBuild)
  }

  val empty = apply(0)

  val ordering: Ordering[BuildVersion] = Ordering.fromLessThan[BuildVersion]((version1, version2) => {
    if (version1.client != version2.client) {
      false
    } else {
      isLessThan(version1.build, version1.localBuild, version2.build, version2.localBuild)
    }
  })

  private def isLessThan(v1: Seq[Int], local1: Option[Int], v2: Seq[Int], local2: Option[Int]): Boolean = {
    for (i <- 0 until v1.size) {
      if (v2.size > i) {
        if (v1(i) < v2(i)) {
          return true
        } else if (v1(i) > v2(i)) {
          return false
        }
      } else {
        return false
      }
    }
    (local1, local2) match {
      case (None, None) =>
        false
      case (None, Some(_)) =>
        true
      case (Some(_), None) =>
        false
      case (Some(local1), Some(local2)) =>
        local1 < local2
    }
  }
}
