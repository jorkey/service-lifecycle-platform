package com.vyulabs.update.lock

import java.io.{File, RandomAccessFile}
import java.nio.channels.FileLock

import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.02.20.
  * Copyright FanDate, Inc.
  */
class SmartFileLock(val shared: Boolean, private[lock] val locker: SmartFileLocker) {
  def release(): Unit = {
    locker.release(this)
  }
}

private[lock] class SmartFileLocker(val file: File, filesAssessor: SmartFilesLocker) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private var lock = Option.empty[FileLock]
  private var exclusivelySmartLock = Option.empty[SmartFileLock]
  private var sharedSmartLocks = Set.empty[SmartFileLock]
  private var closed = false

  def tryLock(shared: Boolean): Option[SmartFileLock] = {
    this.synchronized {
      if (closed || exclusivelySmartLock.isDefined || (!shared && sharedSmartLocks.size != 0)) {
        return None
      } else {
        var randomAccess = Option.empty[RandomAccessFile]
        if (lock.isEmpty) {
          try {
            randomAccess = Some(new RandomAccessFile(file, "rw"))
            lock = randomAccess.get.getChannel.tryLock(0L, Long.MaxValue, shared) match {
              case null => None
              case l => Some(l)
            }

          } catch {
            case ex: Exception =>
              log.error(s"File ${file} locking exception", ex)
              return None
          } finally {
            if (lock.isEmpty) {
              try {
                randomAccess.foreach(_.close())
              } catch {
                case ex: Exception =>
                  log.error(s"File ${file} closing exception", ex)
              }
            }
          }
        }
        if (lock.isDefined) {
          val accessLock = new SmartFileLock(shared, this)
          if (!shared) {
            exclusivelySmartLock = Some(accessLock)
          } else {
            sharedSmartLocks += accessLock
          }
          log.debug(s"Locked ${file.getName}, shared ${shared}")
          Some(accessLock)
        } else {
          None
        }
      }
    }
  }

  def release(accessLock: SmartFileLock): Unit = {
    val needClose = this.synchronized {
      val needClose =
        if (!accessLock.shared) {
          if (!exclusivelySmartLock.contains(accessLock)) {
            log.error(s"File ${file} is not locked")
            return
          }
          exclusivelySmartLock = None
          true
        } else {
          if (!sharedSmartLocks.contains(accessLock)) {
            log.error(s"File ${file} is not locked")
            return
          }
          sharedSmartLocks -= accessLock
          sharedSmartLocks.isEmpty
        }
      if (needClose) {
        closed = true
        for (lock <- lock) {
          try {
            lock.release()
            lock.channel().close()
            this.lock = None
          } catch {
            case ex: Exception =>
              log.warn(s"File ${file} unlocking exception", ex)
          }
        }
      }
      needClose
    }
    if (needClose) {
      filesAssessor.close(this)
    }
    log.debug(s"Unlocked ${file.getName}, shared ${accessLock.shared}")
  }
}
