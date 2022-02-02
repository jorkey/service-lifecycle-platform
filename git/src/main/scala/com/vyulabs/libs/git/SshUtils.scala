package com.vyulabs.libs.git

import java.io.File

import com.jcraft.jsch.ChannelSftp
import org.slf4j.Logger

import collection.JavaConverters._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.01.19.
  * Copyright FanDate, Inc.
  */
object SshUtils {

  def exists(host: String, remoteFile: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        channel.stat(remoteFile)
        true
      } catch {
        case _: Exception =>
          false
      } finally {
        channel.disconnect()
      }
    } catch {
      case ex: Exception =>
        log.error(s"Error of connecting to ${host}", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def realPath(host: String, remoteFile: String)(implicit log: Logger): Option[String] = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        Some(channel.realpath(remoteFile))
      } catch {
        case _: Exception =>
          None
      } finally {
        channel.disconnect()
      }
    } catch {
      case ex: Exception =>
        log.error(s"Error of connecting to ${host}", ex)
        None
    } finally {
      ssh.disconnect()
    }
  }

  def makeDirIfNotExists(host: String, remoteDir: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        val exists = try {
          channel.stat(remoteDir)
          true
        } catch {
          case _: Exception =>
            false
        }
        if (!exists) {
          channel.mkdir(remoteDir)
        }
      } finally {
        channel.disconnect()
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of creating directory ${remoteDir} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def removeDir(host: String, remoteDir: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        val exists = try {
          channel.stat(remoteDir)
          true
        } catch {
          case _: Exception =>
            false
        }
        if (exists) {
          channel.rmdir(remoteDir)
        }
      } finally {
        channel.disconnect()
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of deleting directory ${remoteDir} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def makeLink(host: String, sourceFile: String, targetFile: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        val exists = try {
          channel.stat(targetFile)
          true
        } catch {
          case _: Exception =>
            false
        }
        if (exists) {
          channel.rm(targetFile)
        }
        channel.symlink(sourceFile, targetFile)
      } finally {
        channel.disconnect()
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of link ${targetFile} to ${sourceFile} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def ls(host: String, remoteDir: String)(implicit log: Logger): (Boolean, Seq[String]) = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        val entries = channel.ls(remoteDir).asScala.toVector.asInstanceOf[Vector[ChannelSftp#LsEntry]]
        (true, entries.map(_.getFilename))
      } finally {
        channel.disconnect()
      }
    } catch {
      case ex: Exception =>
        log.error(s"Error of getting directory ${remoteDir} list by SCP", ex)
        (false, Seq.empty)
    } finally {
      ssh.disconnect()
    }
  }

  def getDir(host: String, localDir: File, remoteDir: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      ssh.get(remoteDir, localDir, true)
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of getting directory ${remoteDir} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def putDir(host: String, localDir: File, remoteDir: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val tmpDir = remoteDir + ".tmp"
      val channel = ssh.openSFtpChannel()
      try {
        val tmpDirExists = try {
          channel.stat(tmpDir)
          true
        } catch {
          case _: Exception =>
            false
        }
        if (tmpDirExists) {
          channel.rmdir(tmpDir)
        }
        ssh.put(localDir, tmpDir, true)
        channel.rename(tmpDir, remoteDir)
      } finally {
        channel.disconnect()
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of putting directory ${remoteDir} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def getFile(host: String, inputFile: String, outputFile: File)(implicit log: Logger): Boolean = {
    if (outputFile.exists() && !outputFile.delete()) {
      log.error(s"Can't delete file ${outputFile}")
      return false
    }
    val ssh = SshClient(host)
    try {
      ssh.connect()
      ssh.get(inputFile, outputFile, false)
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of getting file ${inputFile} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }

  def putFile(host: String, inputFile: File, outputFile: String)(implicit log: Logger): Boolean = {
    val ssh = SshClient(host)
    try {
      ssh.connect()
      val channel = ssh.openSFtpChannel()
      try {
        val tmpFile = outputFile + ".tmp"
        ssh.put(inputFile, tmpFile, false)
        channel.rename(tmpFile, outputFile)
      } finally {
        channel.disconnect()
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Error of putting file ${outputFile} by SCP", ex)
        false
    } finally {
      ssh.disconnect()
    }
  }
}
