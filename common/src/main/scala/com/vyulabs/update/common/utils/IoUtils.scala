package com.vyulabs.update.common.utils

import java.io._
import java.util.Date

import com.typesafe.config._
import com.vyulabs.update.common.common.Common
import com.vyulabs.update.common.common.Common.ServiceName
import com.vyulabs.update.common.lock.SmartFilesLocker
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger
import spray.json._

import scala.annotation.tailrec

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object IoUtils {
  def readFileToBytes(file: File)(implicit log: Logger): Option[Array[Byte]] = {
    try {
      val in = new FileInputStream(file)
      val buf = new Array[Byte](file.length().toInt)
      var index = 0
      while (index < buf.length) {
        val ret = in.read(buf, index, buf.length - index)
        if (ret == -1) {
          throw new EOFException()
        }
        index += ret
      }
      in.close()
      Some(buf)
    } catch {
      case ex: Exception =>
        log.error(s"Read file ${file} error", ex)
        None
    }
  }

  def readFileToBytesWithLock(file: File)
                             (implicit filesLocker: SmartFilesLocker, log: Logger): Option[Array[Byte]] = {
    synchronize(file, true, (attempt, time) => {
      Thread.sleep(100)
      true
    }, () => {
      readFileToBytes(file)
    }).getOrElse(None)
  }

  def readFileToJson[T](file: File)(implicit reader: JsonReader[T], log: Logger): Option[T] = {
    readFileToBytes(file).map(new String(_, "utf8").parseJson.convertTo[T])
  }

  def readFileToJsonWithLock[T](file: File)(implicit reader: JsonReader[T], log: Logger): Option[T] = {
    readFileToBytes(file).map(new String(_, "utf8").parseJson.convertTo[T])
  }

  def writeBytesToFile(file: File, data: Array[Byte])(implicit log: Logger): Boolean = {
    try {
      val out = new FileOutputStream(file)
      out.write(data)
      out.close()
      true
    } catch {
      case ex: Exception =>
        log.error(s"Write file ${file} error", ex)
        false
    }
  }

  def writeBytesToFileWithLock(file: File, data: Array[Byte])
                              (implicit filesLocker: SmartFilesLocker, log: Logger): Boolean = {
    synchronize(file, false, (attempt, time) => {
      Thread.sleep(100)
      true
    }, () => {
      writeBytesToFile(file, data)
    }).getOrElse(false)
  }

  def writeJsonToFile[T](file: File, obj: T)(implicit writer: JsonWriter[T], log: Logger): Boolean = {
    writeBytesToFile(file, obj.toJson.sortedPrint.getBytes("utf8"))
  }

  def writeJsonToFileWithLock[T](file: File, obj: T)
                                (implicit writer: JsonWriter[T], filesLocker: SmartFilesLocker, log: Logger): Boolean = {
    writeBytesToFileWithLock(file, obj.toJson.sortedPrint.getBytes("utf8"))
  }

  def writeConfigToFile(file: File, config: Config)(implicit log: Logger): Boolean = {
    val json = file.getName.endsWith(".json")
    writeBytesToFile(file, renderConfig(config, json).getBytes("utf8"))
  }

  def parseConfigFile(file: File, options: ConfigParseOptions = ConfigParseOptions.defaults())(implicit log: Logger): Option[Config] = {
    if (file.exists()) {
      try {
        Some(ConfigFactory.parseFile(file, options))
      } catch {
        case e: Exception =>
          log.error(s"Parse file ${file} error", e)
          None
      }
    } else {
      log.info(s"File ${file} not exists")
      None
    }
  }

  def renderConfig(config: Config, json: Boolean): String = {
    val renderOpts = ConfigRenderOptions.defaults().setFormatted(true).setOriginComments(false).setJson(json)
    config.root().render(renderOpts)
  }

  def copyFile(from: File, to: File, filter: (File) => Boolean = (_) => true, settings: Map[String, String] = Map.empty)
              (implicit log: Logger): Boolean = {
    if (from.isDirectory) {
      if (!to.exists() && !to.mkdir()) {
        log.error(s"Can't make directory ${to}")
        return false
      }
      for (fromChild <- from.listFiles()) {
        if (filter(fromChild)) {
          val toChild = new File(to, fromChild.getName)
          if (!copyFile(fromChild, toChild, filter, settings)) {
            return false
          }
        }
      }
      true
    } else {
      if (!settings.isEmpty) {
        log.info(s"Expand macros from file ${from} and copy to ${to}")
        if (!macroExpansion(from, to, settings)) {
          return false
        }
        true
      } else {
        try {
          log.info(s"Copy file ${from} to ${to}")
          val in = new FileInputStream(from)
          val out = new FileOutputStream(to)
          val buf = new Array[Byte](1024)
          var eof = false
          while (!eof) {
            val ret = in.read(buf, 0, 1024)
            if (ret != -1) {
              out.write(buf, 0, ret)
            } else {
              eof = true
            }
          }
          in.close()
          out.close()
        } catch {
          case ex: Exception =>
            log.error(s"Copy file ${from} to ${to} error", ex)
            return false
        }
        true
      }
    }
  }

  def readTailOfTextFile(file: File, count: Int)(implicit log: Logger): Seq[String] = {
    try {
      val in = new BufferedReader(new FileReader(file))
      var lines = Seq.empty[String]
      var line: String = in.readLine
      while (line != null) {
        lines :+= line
        if (lines.size > count) {
          lines = lines.drop(lines.size - count)
        }
        line = in.readLine
      }
      in.close()
      lines
    } catch {
      case ex: Exception =>
        log.error(s"Read file ${file} error", ex)
        Seq.empty
    }
  }

  def readServiceVersion(serviceName: ServiceName, directory: File)(implicit log: Logger): Option[ClientDistributionVersion] = {
    val versionMarkFile = new File(directory, Common.VersionMarkFile.format(serviceName))
    if (versionMarkFile.exists()) {
      val bytes = readFileToBytes(versionMarkFile).getOrElse {
        return None
      }
      val str = new String(bytes, "utf8").trim
      try {
        val version = ClientDistributionVersion.parse(str)
        Some(version)
      } catch {
        case ex: Exception =>
          log.error(s"Can't parse ${str}", ex)
          None
      }
    } else {
      None
    }
  }

  def getServiceInstallTime(serviceName: ServiceName, directory: File)(implicit log: Logger): Option[Date] = {
    val versionMarkFile = new File(directory, Common.VersionMarkFile.format(serviceName))
    if (versionMarkFile.exists()) {
      Some(new Date(versionMarkFile.lastModified()))
    } else {
      None
    }
  }

  def writeServiceVersion(directory: File, serviceName: ServiceName, version: ClientDistributionVersion)(implicit log: Logger): Boolean = {
    val versionMarkFile = new File(directory, Common.VersionMarkFile.format(serviceName))
    writeBytesToFile(versionMarkFile, version.toString.getBytes("utf8"))
  }

  def macroExpansion(inputFile: File, outputFile: File, args: Map[String, String])(implicit log: Logger): Boolean = {
    val bytes = readFileToBytes(inputFile).getOrElse {
      return false
    }
    var contents = new String(bytes, "utf8")
    for (entry <- args) {
      val variable = entry._1
      val value = entry._2
      contents = contents.replaceAll(s"%%${variable}%%", value)
    }
    writeBytesToFile(outputFile, contents.getBytes("utf8"))
  }

  def deleteFileRecursively(file: File): Boolean = {
    val contents = file.listFiles()
    if (contents != null) {
      for (file <- contents) {
        if (!deleteFileRecursively(file)) {
          return false
        }
      }
    }
    file.delete()
  }

  def listDirectory(directory: File)(implicit log: Logger): Option[Seq[File]] = {
    try {
      Some(directory.listFiles().toSeq)
    } catch {
      case e: Exception =>
        log.error(s"List directory ${directory} error", e)
        None
    }
  }

  def makeEmptyDirectory(directory: File)(implicit log: Logger): Boolean = {
    if (!directory.exists()) {
      if (!directory.mkdir()) {
        log.error(s"Can't make directory ${directory}")
        return false
      }
    } else if (!IoUtils.deleteDirectoryContents(directory)) {
      log.error(s"Can't delete directory ${directory} contents")
      return false
    }
    true
  }

  def deleteDirectoryContents(file: File): Boolean = {
    val contents = file.listFiles()
    if (contents != null) {
      for (file <- contents) {
        if (!deleteFileRecursively(file)) {
          return false
        }
      }
    }
    true
  }

  def synchronize[T](file: File, shared: Boolean, failureHandler: (Int, Long) => Boolean, successHandler: => () => T)
                    (implicit filesLocker: SmartFilesLocker, log: Logger): Option[T] = {
    var attempt = 1
    val startTime = System.currentTimeMillis()
    while (true) {
      filesLocker.tryLock(file, shared) match {
        case Some(lock) =>
          try {
            return Some(successHandler())
          } finally {
            lock.release()
          }
        case None =>
          if (!failureHandler(attempt, System.currentTimeMillis() - startTime)) {
            return None
          }
          attempt += 1
      }
    }
    None
  }

  def getUsedSpace(file: File): Long = {
    if (file.isFile) {
      file.length()
    } else {
      var space = 0L
      val contents = file.listFiles()
      if (contents != null) {
        for (file <- contents) {
          space += getUsedSpace(file)
        }
      }
      space
    }
  }

  @tailrec
  def maybeFreeSpace(dir: File, maxCapacity: Long, except: Set[File])(implicit log: Logger): Unit = {
    val used = getUsedSpace(dir)
    if (used > maxCapacity) {
      val files = dir.listFiles().filterNot(except.contains(_))
      if (files.length > 1) {
        val oldestFile = files.sortBy(_.lastModified()).head
        if (!deleteFileRecursively(oldestFile)) {
          log.error(s"Can't delete ${oldestFile}")
        } else {
          maybeFreeSpace(dir, maxCapacity, except)
        }
      }
    }
  }

  def maybeDeleteExcessFiles(dir: File, maxCount: Int, except: Set[File])(implicit log: Logger): Unit = {
    val files = dir.listFiles().filter(_.isFile)
    if (files.length > maxCount) {
      for (file <- files.sortBy(_.lastModified()).take(files.length - maxCount)) {
        if (!except.contains(file) && !deleteFileRecursively(file)) {
          log.error(s"Can't delete ${file}")
        }
      }
    }
  }

  def maybeDeleteOldFiles(dir: File, beforeTime: Long, except: Set[File])(implicit log: Logger): Unit = {
    val files = dir.listFiles().filter(_.isFile)
      for (file <- files.filter(_.lastModified() < beforeTime)) {
        if (!except.contains(file) && !deleteFileRecursively(file)) {
          log.error(s"Can't delete ${file}")
        }
      }
  }

  def listFiles(file: File, path: String = ""): Seq[String] = {
    if (file.isDirectory) {
      file.listFiles().foldLeft(Seq.empty[String])((list, file) => list ++ listFiles(file, path + file.getName +
        (if (file.isDirectory) "/" else "")))
    } else {
      Seq(path)
    }
  }
}
