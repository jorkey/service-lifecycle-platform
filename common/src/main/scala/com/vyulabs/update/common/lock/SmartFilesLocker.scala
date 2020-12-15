package com.vyulabs.update.common.lock

import java.io.File

import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.02.20.
  * Copyright FanDate, Inc.
  */
class SmartFilesLocker {
  private var files = Map.empty[File, SmartFileLocker]

  def tryLock(file: File, shared: Boolean)(implicit log: Logger): Option[SmartFileLock] = {
    this.synchronized {
      files.get(file) match {
        case Some(accessor) =>
          accessor.tryLock(shared)
        case None =>
          val accessor = new SmartFileLocker(file, this)
          accessor.tryLock(shared) match {
            case Some(lock) =>
              files += (file -> accessor)
              Some(lock)
            case None =>
              None
          }
      }
    }
  }

  private [lock] def close(accessor: SmartFileLocker): Unit = {
    this.synchronized {
      files -= accessor.file
    }
  }
}
