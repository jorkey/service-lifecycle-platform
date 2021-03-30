package com.vyulabs.update.common.version

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 26.04.19.
  * Copyright FanDate, Inc.
  */

object Build {
  val empty = Seq(0)
  val initialBuild = Seq(1, 0, 0)

  val ordering: Ordering[Seq[Int]] = Ordering.fromLessThan[Seq[Int]]((build1, build2) => {
    isLessThan(build1, build2)
  })

  def parse(build: String): Seq[Int] = {
    val b = build.split("\\.").map(_.toInt)
    if (b.size == 0) {
      throw new IllegalArgumentException(s"Invalid build ${build}")
    }
    b
  }

  def toString(build: Seq[Int]): String = {
    build.foldLeft("")((s, v) => if (s.isEmpty) v.toString else s + "." + v.toString)
  }

  def next(build: Seq[Int]) = {
    val v = build.last + 1
    build.dropRight(1) :+ v
  }

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
