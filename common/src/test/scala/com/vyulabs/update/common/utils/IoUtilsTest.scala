package com.vyulabs.update.common.utils

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import java.io.File
import java.nio.file.Files

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.02.19.
  * Copyright FanDate, Inc.
  */
class IoUtilsTest extends FlatSpec with Matchers {
  behavior of "IoUtils"

  implicit val log = LoggerFactory.getLogger(this.getClass)

  it should "free directory space" in {
    val dir = Files.createTempDirectory("io-utils-test").toFile
    val dir1 = new File(dir, "dir1"); assert(dir1.mkdir())
    val dir2 = new File(dir, "dir2"); assert(dir2.mkdir())
    val dir3 = new File(dir, "dir3"); assert(dir3.mkdir())

    assert(IoUtils.writeBytesToFile(new File(dir1, "file1"), new Array[Byte](5000)))
    val dir11 = new File(dir1, "dir1"); assert(dir11.mkdir())
    assert(IoUtils.writeBytesToFile(new File(dir11, "file2"), new Array[Byte](2000)))
    assert(IoUtils.writeBytesToFile(new File(dir1, "file3"), new Array[Byte](3000)))

    assert(IoUtils.writeBytesToFile(new File(dir2, "file1"), new Array[Byte](1000)))
    assert(IoUtils.writeBytesToFile(new File(dir2, "file2"), new Array[Byte](6000)))
    val dir21 = new File(dir2, "dir1"); assert(dir21.mkdir())
    assert(IoUtils.writeBytesToFile(new File(dir21, "file3"), new Array[Byte](3000)))

    assert(IoUtils.writeBytesToFile(new File(dir3, "file1"), new Array[Byte](2000)))
    assert(IoUtils.writeBytesToFile(new File(dir3, "file2"), new Array[Byte](2000)))
    assert(IoUtils.writeBytesToFile(new File(dir3, "file3"), new Array[Byte](6000)))

    IoUtils.maybeFreeSpace(dir, 20000, Set.empty)
    assert(!dir1.exists())
    assert(dir2.exists())
    assert(dir3.exists())
  }
}
