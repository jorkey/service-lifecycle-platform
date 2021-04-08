package com.vyulabs.libs.git

import java.io._
import java.util.Properties

import com.jcraft.jsch._

import scala.util.control.Breaks._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
class SshClient(host: String, user: String, port: Int = 22)(implicit log: org.slf4j.Logger) {
  private val sch = new JSch()

  private val home = System.getenv("HOME")
  private val config = OpenSSHConfig.parseFile(s"${home}/.ssh/config")

  sch.setConfigRepository(config)
  sch.addIdentity(s"${home}/.ssh/id_rsa")

  private val session = sch.getSession(user, host, port)

  private val sessionConfig = new Properties
  sessionConfig.put("StrictHostKeyChecking", "no")
  session.setConfig(sessionConfig)

  private val buf = new Array[Byte](1024)

  @throws(classOf[IOException])
  def connect(): Unit = {
    session.connect()
  }

  def disconnect(): Unit = {
    session.disconnect()
  }

  def openSFtpChannel(): ChannelSftp = {
    val channel = session.openChannel("sftp").asInstanceOf[ChannelSftp]
    channel.connect()
    channel
  }

  @throws(classOf[IOException])
  def get(remoteFile: String, localFile: File, recursively: Boolean = true): Unit = {
    val command: String = s"scp ${if (recursively) " -r" else ""} -f ${remoteFile}"
    val channel = session.openChannel("exec")

    channel.asInstanceOf[ChannelExec].setCommand(command)
    channel.connect()

    val out = channel.getOutputStream
    val in = channel.getInputStream

    try {
      sendAck(out)
      getFile(in, out, localFile, true)
    } finally {
      channel.disconnect()
    }
  }

  @throws(classOf[IOException])
  def put(localFile: File, remoteFile: String, recursively: Boolean = true): Unit = {
    val command: String = s"scp ${if (recursively) " -r" else ""} -t ${remoteFile}"
    val channel = session.openChannel("exec")

    channel.asInstanceOf[ChannelExec].setCommand(command)
    channel.connect()

    val out = channel.getOutputStream
    val in = channel.getInputStream

    try {
      checkAck(in)
      putFile(in, out, localFile)
    } finally {
      channel.disconnect()
    }
  }

  private def getFile(in: InputStream, out: OutputStream, output: File, first: Boolean): Unit = {
    breakable { while (true) {
      val fileType = readAck(in)
      if (log.isDebugEnabled) log.debug(s"file type ${fileType.toChar}")
      if (fileType != -1) {
        fileType match {
          case 'D' | 'C' =>
            readFilePermissions(in)
            val size = readFileSize(in)
            val fileName = readFileName(in)
            val file = if (first) output else new File(output, fileName)
            sendAck(out)
            if (fileType == 'D') {
              if (log.isDebugEnabled) log.debug(s"mkdir ${file}")
              if (!file.exists() && !file.mkdir()) {
                throw new IOException(s"Can't make directory ${file}")
              }
              getFile(in, out, file, false)
            } else {
              if (log.isDebugEnabled) log.debug(s"write file ${file}")
              val fileOut = new FileOutputStream(file)
              readFile(in, fileOut, size)
              sendAck(out)
            }
          case 'E' =>
            readCR(in)
            sendAck(out)
            break
        }
      } else {
        break
      }
    }}
  }

  private def putFile(in: InputStream, out: OutputStream, file: File): Unit = {
    out.write(if (file.isDirectory) 'D' else 'C')
    writeFilePermissions(out, file)
    out.write(' ')
    writeFileSize(out, file)
    out.write(' ')
    writeFileName(out, file)
    out.write(0x0a)
    out.flush()
    checkAck(in)
    if (file.isDirectory) {
      log.debug(s"read directory ${file}")
      for (file <- file.listFiles()) {
        putFile(in, out, file)
      }
      out.write('E')
      out.write('\n')
      out.flush()
      checkAck(in)
    } else {
      log.debug(s"read file ${file}")
      val fileIn = new FileInputStream(file)
      writeFile(fileIn, out, file.length())
      checkAck(in)
    }
  }

  private def readFilePermissions(in: InputStream): Unit = {
    in.read(buf, 0, 5)
    log.debug("permissions " + new String(buf.drop(5), "utf8"))
  }

  private def writeFilePermissions(out: OutputStream, file: File): Unit = {
    if (file.isDirectory) {
      out.write("0775".getBytes)
    } else {
      out.write("0664".getBytes)
    }
  }

  private def readFileSize(in: InputStream): Long = {
    var size = 0L
    breakable { while (true) {
      if (in.read(buf, 0, 1) < 0) {
        break
      }
      if (buf(0) == ' ') {
        break
      }
      size = size * 10L + (buf(0) - '0').toLong
    }}
    size
  }

  private def writeFileSize(out: OutputStream, file: File): Unit = {
    out.write(file.length().toString.getBytes)
  }

  private def readFileName(in: InputStream): String = {
    var i = 0
    breakable { while (true) {
      if (in.read(buf, i, 1) == -1) {
        throw new EOFException()
      }
      if (buf(i) == 0x0a.toByte) {
        break
      }
      i += 1
    }}
    new String(buf, 0, i)
  }

  private def writeFileName(out: OutputStream, file: File): Unit = {
    out.write(file.getName.getBytes("utf8"))
  }

  private def readCR(in: InputStream): Unit = {
    if (in.read(buf, 0, 1) == -1) {
      throw new EOFException()
    }
    if (buf(0) != 0x0a.toByte) {
      throw new IOException(s"Unexpected ${buf(0)} instead of CR")
    }
  }

  private def readFile(in: InputStream, out: OutputStream, fileSize: Long): Unit = {
    write(in, out, fileSize)
    out.close()
    checkAck(in)
  }

  private def writeFile(in: InputStream, out: OutputStream, fileSize: Long): Unit = {
    write(in, out, fileSize)
    sendAck(out)
  }

  private def write(in: InputStream, out: OutputStream, fileSize: Long): Unit = {
    var size = fileSize
    breakable { while (true) {
      val blockSize = if (buf.length < size) {
        buf.length
      } else {
        size.toInt
      }
      val ret = in.read(buf, 0, blockSize)
      if (ret < 0) {
        throw new EOFException()
      }
      out.write(buf, 0, ret)
      size -= ret
      if (size == 0L) {
        break
      }
    }}
  }

  private def readAck(in: InputStream): Int = {
    val b = in.read
    // byte may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //          -1
    if (b == 1 || b == 2) {
      val sb = new StringBuffer
      var c = 0
      do {
        c = in.read
        sb.append(c.toChar)
      } while (c != '\n')
      throw new IOException(sb.toString)
    }
    b
  }

  private def checkAck(in: InputStream): Unit = {
    if (readAck(in) != 0) {
      throw new IOException("No ACK")
    }
  }

  private def sendAck(out: OutputStream, error: Option[String] = None): Unit = {
    error match {
      case None =>
        out.write(0)
      case Some(error) =>
        out.write(2)
        out.write(error.getBytes)
        out.write('\n')
    }
    out.flush()
  }
}

object SshClient {
  def apply(host: String, user: String = "", port: Int = 22)(implicit log: org.slf4j.Logger): SshClient = {
    new SshClient(host, user, port)
  }
}