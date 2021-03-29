package com.vyulabs.update.common.version

import spray.json.{JsString, JsValue, RootJsonFormat}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

case class Version(build: Seq[Int]) {
  def isEmpty(): Boolean = {
    build.foreach { v => if (v != 0) return false }
    true
  }

  def next() = {
    val v = build.last + 1
    Version(build.dropRight(1) :+ v)
  }

  override def toString: String = {
    build.foldLeft("")((s, v) => if (s.isEmpty) v.toString else s + "." + v.toString)
  }
}

object Version {
  val initialVersion = Version(Seq(1, 0, 0))

  implicit object BuildVersionJsonFormat extends RootJsonFormat[Version] {
    def write(value: Version) = JsString(value.toString)
    def read(value: JsValue) = Version.parse(value.asInstanceOf[JsString].value)
  }

  def parse(version: String): Version = {
    val build = version.split("\\.").map(_.toInt)
    if (build.size == 0) {
      throw new IllegalArgumentException(s"Invalid version ${version}")
    }
    new Version(build)
  }

  val empty = apply(Seq(0))

  val ordering: Ordering[Version] = Ordering.fromLessThan[Version]((version1, version2) => {
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