package com.vyulabs.libs.git

import java.io.{File, FileInputStream, FileOutputStream, IOException}

import org.slf4j.Logger

class GitLock(git: GitRepository, name: String)(implicit log: Logger) extends Thread {
  private val updateLockTimeoutMs = 15000
  private val checkLockTimeoutMs = 10000
  private val lockExpirationTimeoutMs = 60000

  private val file = new File(git.getDirectory(), name + ".lock")

  private var continueMessage = new String

  @volatile
  private var toUnlock = false

  def lock(startMessage: String, continueMessage: String): Boolean = {
    if (isAlive) {
      log.error(s"""Resource ${name} is already locked""")
      return false
    }
    this.continueMessage = continueMessage
    if (tryToLock(startMessage)) {
      return true
    } else {
      log.error(s"""Resource ${name} is locked - wait ...""")
    }
    do {
      Thread.sleep(checkLockTimeoutMs)
      if (!git.pull()) {
        return false
      }
    } while (!tryToLock(startMessage))
    start()
    true
  }

  def unlock(endMessage: String): Boolean = {
    this.synchronized {
      toUnlock = true
      notify()
    }
    join()
    if (!git.remove(file)) {
      log.error(s"Can't remove lock of ${name}")
      return false
    }
    if (!git.commit(endMessage)) {
      return false
    }
    git.push()
  }

  override def run: Unit = {
    this.synchronized {
      while (!toUnlock) {
        wait(updateLockTimeoutMs)
        if (!update(continueMessage, true)) {
          log.error(s"Can't update lock of ${name}")
        }
      }
    }
  }

  private def tryToLock(commitMessage: String): Boolean = {
    if (file.exists()) {
      val time = try {
        val in = new FileInputStream(file)
        val bytes = new Array[Byte](20)
        val len = in.read(bytes)
        in.close()
        new String(bytes, 0, len, "utf8").toLong
      } catch {
        case ex: Exception =>
          log.error(s"Read lock file ${file} exception", ex)
          return false
      }
      val currentTime = System.currentTimeMillis()
      if (currentTime - time < lockExpirationTimeoutMs) {
        return false
      }
    }
    update(commitMessage, true)
  }

  private def update(commitMessage: String, retryWithPull: Boolean): Boolean = {
    val time = System.currentTimeMillis()
    try {
      val out = new FileOutputStream(file)
      out.write(time.toString.getBytes)
      out.close()
    } catch {
      case ex: IOException =>
        log.error(s"Write lock file ${file} exception", ex)
        return false
    }
    if (!git.add(file)) {
      return false
    }
    if (!git.commit(commitMessage)) {
      return false
    }
    git.push(List.empty, retryWithPull)
  }
}
