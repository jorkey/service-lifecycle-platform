package com.vyulabs.update.log

import java.io._

import scala.collection.immutable.Queue

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 7.05.19.
  * Copyright FanDate, Inc.
  */
class LogWriter(directory: File,
                maxFileSize: Long, maxFilesCount: Int, filePrefix: String,
                error: (String, Exception) => Unit) {

  private var writer = Option.empty[BufferedWriter]

  private var logTail = Queue.empty[String]
  private val logTailSize = 250

  if (!directory.exists()) {
    directory.mkdirs()
  }

  shift()
  openWriter()

  def close(): Unit = {
    closeWriter()
  }

  def writeLogLine(line: String, flush: Boolean = true): Unit = {
    addLogLineToTail(line)
    for (writer <- writer) {
      try {
        writer.write(line)
        writer.newLine()
        if (flush) {
          writer.flush()
        }
      } catch {
        case ex: IOException =>
          error("Write log file error", ex)
      }
    }
    if (getLogFile(0).length()  >= maxFileSize) {
      shift()
    }
  }

  def flush(): Unit = {
    for (writer <- writer) {
      writer.flush()
    }
  }

  def getLogTail() = logTail

  private def shift(): Unit = {
    closeWriter()
    for (index <- maxFilesCount-1 until 0 by -1) {
      val file = getLogFile(index)
      if (file.exists()) {
        file.delete()
      }
      val prevFile = getLogFile(index - 1)
      if (prevFile.exists()) {
        prevFile.renameTo(file)
      }
    }
    openWriter()
  }

  private def openWriter(): Unit = {
    try {
      writer = Some(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getLogFile(0), true))))
    } catch {
      case ex: IOException =>
        error("Open log file error", ex)
    }
  }

  private def closeWriter(): Unit = {
    for (writer <- writer) {
      try {
        writer.close()
      } catch {
        case ex: IOException =>
          error("Close log file error", ex)
      }
      this.writer = None
    }
  }

  private def getLogFile(index: Int): File = {
    if (index == 0) {
      new File(directory, s"${filePrefix}.log")
    } else {
      new File(directory, s"${filePrefix}-${index}.log")
    }
  }

  private def addLogLineToTail(line: String): Unit = {
    synchronized {
      logTail = logTail.enqueue(line)
      if (logTail.size > logTailSize) {
        logTail = logTail.dequeue._2
      }
    }
  }
}
