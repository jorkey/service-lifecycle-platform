package com.vyulabs.update.common.utils

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import org.slf4j.Logger

object ZipUtils {
  def zip(zipFile: File, inputFile: File)(implicit log: Logger): Boolean = {
    log.debug(s"Zip ${inputFile}")
    var zipOutput: ZipOutputStream = null
    try {
      zipOutput = new ZipOutputStream(new FileOutputStream(zipFile))
      if (inputFile.isDirectory) {
        zip(zipOutput, inputFile, inputFile)
      } else {
        zip(zipOutput, new File(""), inputFile)
      }
    } catch {
      case ex: Exception =>
        log.error("Zip exception", ex)
        false
    } finally {
      if (zipOutput != null) {
        zipOutput.close()
      }
    }
  }

  def zip(zipFile: File, inputFiles: Seq[File])(implicit log: Logger): Boolean = {
    log.debug(s"Zip ${inputFiles}")
    var zipOutput: ZipOutputStream = null
    try {
      zipOutput = new ZipOutputStream(new FileOutputStream(zipFile))
      for (inputFile <- inputFiles) {
        if (!zip(zipOutput, new File(inputFile.getParent), inputFile)) {
          return false
        }
      }
      true
    } catch {
      case ex: Exception =>
        log.error("Zip exception", ex)
        false
    } finally {
      if (zipOutput != null) {
        zipOutput.close()
      }
    }
  }

  def zip(zipOutput: ZipOutputStream, rootFile: File, inputFile: File)(implicit log: Logger): Boolean = {
    try {
      var zipName = rootFile.toPath.relativize(inputFile.toPath).toString
      if (!zipName.isEmpty) {
        if (inputFile.isDirectory) zipName += "/"
        if (log.isDebugEnabled) log.debug(s"Zip ${zipName}")
        val entry = new ZipEntry(zipName)
        entry.setSize(inputFile.length())
        entry.setTime(inputFile.lastModified())
        zipOutput.putNextEntry(entry)
        if (inputFile.isDirectory) {
          zipOutput.closeEntry()
        }
      }
      if (inputFile.isDirectory) {
        for (file <- inputFile.listFiles()) {
          if (!zip(zipOutput, rootFile, file)) {
            return false
          }
        }
      } else {
        val buffer = new Array[Byte](1024)
        val fileInput = new FileInputStream(inputFile)
        try {
          var len = fileInput.read(buffer)
          while (len > 0) {
            zipOutput.write(buffer, 0, len)
            len = fileInput.read(buffer)
          }
          zipOutput.closeEntry()
        } finally {
          fileInput.close()
        }
      }
      true
    } catch {
      case ex: Exception =>
        log.error(s"Zip ${inputFile} exception", ex)
        false
    }
  }

  def unzip(zipFile: File, outputFile: File, map: (String) => Option[String] = Some(_))(implicit log: Logger): Boolean = {
    log.debug(s"Unzip to ${outputFile}")
    var zipInput: ZipInputStream = null
    try {
      if (log.isDebugEnabled) log.debug(s"Unzip ${zipFile} to ${outputFile}")
      zipInput = new ZipInputStream(new FileInputStream(zipFile))
      unzip(zipInput, outputFile, map)
    } catch {
      case ex: Exception =>
        log.error(s"Unzip ${zipFile} exception", ex)
        false
    } finally {
      if (zipInput != null) {
        zipInput.close()
      }
    }
  }

  def unzip(zipInput: ZipInputStream, outputFile: File, map: (String) => Option[String])(implicit log: Logger): Boolean = {
    log.debug(s"Unzip to ${outputFile}")
    try {
      var entry = zipInput.getNextEntry
      while (entry != null) {
        for (outputName <- map(entry.getName)) {
          val file = new File(outputFile + File.separator + outputName)
          if (entry.isDirectory) {
            if (!file.exists() && !file.mkdirs()) {
              log.error(s"Can't make directory ${file}")
              return false
            }
          } else {
            val buffer = new Array[Byte](1024)
            val fileOutput = new FileOutputStream(file)
            try {
              var len = zipInput.read(buffer)
              while (len > 0) {
                fileOutput.write(buffer, 0, len)
                len = zipInput.read(buffer)
              }
            } finally {
              fileOutput.close()
            }
          }
        }
        zipInput.closeEntry()
        entry = zipInput.getNextEntry
      }
      true
    } catch {
      case ex: Exception =>
        log.error("Unzip exception", ex)
        false
    }
  }

  def zipAndSend(directory: File, send: (File) => Boolean)(implicit log: Logger): Boolean = {
    val tmpFile = File.createTempFile("update", "zip")
    try {
      zip(tmpFile, directory) && send(tmpFile)
    } finally {
      tmpFile.delete()
    }
  }

  def receiveAndUnzip(receive: (File) => Boolean, directory: File)(implicit log: Logger): Boolean = {
    val tmpFile = File.createTempFile("update", "zip")
    try {
      receive(tmpFile) && unzip(tmpFile, directory)
    } finally {
      tmpFile.delete()
    }
  }
}
