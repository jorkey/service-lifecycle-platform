package com.vyulabs.update.common.lock

import java.nio.file.Files

import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.02.19.
  * Copyright FanDate, Inc.
  */
class SmartFileLockTest extends FlatSpec with Matchers {
  behavior of "SmartFileLock"

  implicit val filesLocker = new SmartFilesLocker()
  implicit val log = LoggerFactory.getLogger(this.getClass)

  it should "exclusively lock file" in {
    val file = Files.createTempFile("test", "smart-lock").toFile
    file.deleteOnExit()
    val lock1 = filesLocker.tryLock(file, false)
    assert(!lock1.isEmpty)
    assertResult(None)(filesLocker.tryLock(file, false))
    assertResult(None)(filesLocker.tryLock(file, true))
    lock1.get.release()
    val lock2 = filesLocker.tryLock(file, false)
    assert(!lock2.isEmpty)
  }

  it should "shared lock file" in {
    val file = Files.createTempFile("test", "smart-lock").toFile
    file.deleteOnExit()
    val lock1 = filesLocker.tryLock(file, true)
    assert(!lock1.isEmpty)
    val lock2 = filesLocker.tryLock(file, true)
    assert(!lock2.isEmpty)
    assert(lock1.get.locker == lock2.get.locker)
    assertResult(None)(filesLocker.tryLock(file, false))
    lock1.get.release()
    assertResult(None)(filesLocker.tryLock(file, false))
    lock2.get.release()
    val lock3 = filesLocker.tryLock(file, false)
    assert(!lock3.isEmpty)
  }
}
