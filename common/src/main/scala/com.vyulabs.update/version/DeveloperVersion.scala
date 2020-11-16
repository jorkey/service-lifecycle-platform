package com.vyulabs.update.version

import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class DeveloperVersion(build: Seq[Int]) {
  def isEmpty(): Boolean = {
    build.foreach { v => if (v != 0) return false }
    true
  }

  def original(): DeveloperVersion = {
    DeveloperVersion(build)
  }

  def next() = {
    val v = build.last + 1
    DeveloperVersion(build.dropRight(1) :+ v)
  }

  override def toString: String = {
    build.foldLeft("")((s, v) => if (s.isEmpty) v.toString else s + "." + v.toString)
  }
}

object DeveloperVersion {
  implicit object DeveloperVersionJsonFormat extends RootJsonFormat[DeveloperVersion] {
    def write(value: DeveloperVersion) = JsString(value.toString)
    def read(value: JsValue) = DeveloperVersion.parse(value.asInstanceOf[JsString].value)
  }

  def parse(version: String): DeveloperVersion = {
    val build = version.split("\\.").map(_.toInt)
    if (build.size == 0) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    new DeveloperVersion(build)
  }

  val empty = apply(Seq(0))

  val ordering: Ordering[DeveloperVersion] = Ordering.fromLessThan[DeveloperVersion]((version1, version2) => {
    isLessThan(version1.build, version2.build)
  })

  def isLessThan(v1: Seq[Int], v2: Seq[Int]): Boolean = {
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
    true
  }
}