package com.vyulabs.update.utils

import java.io._
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import java.util.jar.Attributes
import java.util.zip.{ZipEntry, ZipInputStream, ZipOutputStream}

import com.typesafe.config._
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.ServiceName
import com.vyulabs.update.config.CommandConfig
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import scala.annotation.tailrec
import scala.collection.JavaConverters._

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 24.12.18.
  * Copyright FanDate, Inc.
  */
object Utils {

  def runProcess(config: CommandConfig, args: Map[String, String],
                 dir: File, logOutput: Boolean)(implicit log: Logger): Boolean = {
    val directory = config.directory match {
      case Some(directory) => new File(dir, directory)
      case None => dir
    }
    runProcess(extendMacro(config.command, args),
      config.args.map(extendMacro(_, args)),
      config.env.mapValues(extendMacro(_, args)),
      directory, config.exitCode, config.outputMatch.map(extendMacro(_, args)), logOutput)
  }

  def runProcess(command: String, args: Seq[String], env: Map[String, String],
                 dir: File, exitCodeMatch: Option[Int],
                 outputMatch: Option[String], logOutput: Boolean)(implicit log: Logger): Boolean = {
    log.info(s"Executing ${command} with arguments ${args} in directory ${dir}")
    try {
      val builder = new ProcessBuilder((command +: args).toList.asJava)
      builder.redirectErrorStream(true)
      env.foldLeft(builder.environment())((e, entry) => {
        if (entry._2 != null) {
          e.put(entry._1, entry._2)
        } else {
          e.remove(entry._1)
        }
        e
      })
      log.debug(s"Environment: ${builder.environment()}")
      builder.directory(dir)
      val proc = builder.start()

      val output = readOutputToString(proc.getInputStream, logOutput)
      if (!logOutput && !output.isEmpty) {
        log.debug(s"Output: ${output}")
      }

      val exitCode = proc.waitFor()
      log.debug(s"Exit code: ${exitCode}")

      for (exitCodeMatch <- exitCodeMatch) {
        if (exitCode != exitCodeMatch) {
          log.error(s"Exit code ${exitCode} does not match expected ${exitCodeMatch}")
          return false
        }
      }
      for (outputMatch <- outputMatch) {
        if (!output.matches(outputMatch)) {
          log.error(s"Output does not match ${outputMatch}")
          return false
        }
      }
      log.info("Command executed successfully")
      true
    } catch {
      case ex: Exception =>
        log.error(s"Start process ${command} error: ${ex.getMessage}", ex)
        false
    }
  }

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

  def writeFileFromBytes(file: File, data: Array[Byte])(implicit log: Logger): Boolean = {
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

  def parseConfigString(string: String, options: ConfigParseOptions = ConfigParseOptions.defaults())(implicit log: Logger): Option[Config] = {
    try {
      Some(ConfigFactory.parseReader(new StringReader(string), options))
    } catch {
      case e: Exception =>
        log.error(s"Parse stream error", e)
        None
    }
  }

  def parseConfigReader(reader: Reader, options: ConfigParseOptions = ConfigParseOptions.defaults())(implicit log: Logger): Option[Config] = {
    try {
      Some(ConfigFactory.parseReader(reader, options))
    } catch {
      case e: Exception =>
        log.error(s"Parse stream error", e)
        None
    }
  }

  def parseConfigFileWithLock(file: File,
                              options: ConfigParseOptions = ConfigParseOptions.defaults())
                             (implicit filesLocker: SmartFilesLocker, log: Logger): Option[Config] = {
    synchronize(file, true, (attempt, time) => {
      Thread.sleep(100)
      true
    }, () => {
      parseConfigFile(file, options)
    }).flatten
  }

  def renderConfig(config: Config, json: Boolean): String = {
    val renderOpts = ConfigRenderOptions.defaults().setFormatted(true).setOriginComments(false).setJson(json)
    config.root().render(renderOpts)
  }

  def writeConfigFile(file: File, config: Config)(implicit log: Logger): Boolean = {
    val json = file.getName.endsWith(".json")
    Utils.writeFileFromBytes(file, renderConfig(config, json).getBytes("utf8"))
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

  def isServiceNeedUpdate(serviceName: ServiceName,
                          ownVersion: Option[BuildVersion], desiredVersion: Option[BuildVersion])(implicit log: Logger): Option[BuildVersion] = {
    ownVersion match {
      case Some(version) if (!version.isEmpty()) =>
        desiredVersion match {
          case Some(desiredVersion) if !BuildVersion.ordering.equiv(version, desiredVersion) =>
            log.info(s"Service ${serviceName} is obsolete. Own version ${version} desired version ${desiredVersion}")
            Some(desiredVersion)
          case Some(_) =>
            log.debug(s"Service ${serviceName} is up to date")
            None
          case None =>
            log.warn(s"No desired version for ${serviceName}")
            None
        }
      case None =>
        None
    }
  }

  def getScriptsVersion(): Option[BuildVersion] = {
    readServiceVersion(new File("."), Common.ScriptsServiceName)
  }

  def readServiceVersion(directory: File, serviceName: ServiceName)(implicit log: Logger): Option[BuildVersion] = {
    val versionMarkFile = new File(directory, Common.VersionMarkFile.format(serviceName))
    if (versionMarkFile.exists()) {
      val bytes = readFileToBytes(versionMarkFile).getOrElse {
        return None
      }
      val str = new String(bytes, "utf8").trim
      try {
        val version = BuildVersion.parse(str)
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

  def writeServiceVersion(directory: File, serviceName: ServiceName, version: BuildVersion)(implicit log: Logger): Boolean = {
    val versionMarkFile = new File(directory, Common.VersionMarkFile.format(serviceName))
    writeFileFromBytes(versionMarkFile, version.toString.getBytes("utf8"))
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
    writeFileFromBytes(outputFile, contents.getBytes("utf8"))
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
    } else if (!Utils.deleteDirectoryContents(directory)) {
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

  def zip(zipFile: File, inputFile: File)(implicit log: Logger): Boolean = {
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

  def unzip(zipFile: File, outputFile: File, filter: (String) => Boolean = _ => true)(implicit log: Logger): Boolean = {
    var zipInput: ZipInputStream = null
    try {
      if (log.isDebugEnabled) log.debug(s"Unzip ${zipFile} to ${outputFile}")
      zipInput = new ZipInputStream(new FileInputStream(zipFile))
      unzip(zipInput, outputFile, filter)
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

  def unzip(zipInput: ZipInputStream, outputFile: File, filter: (String) => Boolean)(implicit log: Logger): Boolean = {
    try {
      var entry = zipInput.getNextEntry
      while (entry != null) {
        if (filter(entry.getName)) {
          val file = new File(outputFile + File.separator + entry.getName)
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
        return false
    }
  }

  def getManifestBuildVersion(product: String)(implicit log: Logger): Option[BuildVersion] = {
    try {
      val resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF")
      while (resources.hasMoreElements()) {
        val manifest = new java.util.jar.Manifest(resources.nextElement().openStream())
        val attrs = manifest.getMainAttributes()
        val titleKey = new Attributes.Name(("Implementation-Title"))
        if (attrs.containsKey(titleKey)) {
          val title = attrs.getValue(titleKey)
          if (title == product) {
            val versionKey = new Attributes.Name("Implementation-Version")
            val version = attrs.getValue(versionKey)
            return Some(BuildVersion.parse(version))
          }
        }
      }
      None
    } catch {
      case ex: IOException =>
        log.error("Read manifest exception", ex)
        None
    }
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

  def extendMacro(macroString: String, args: Map[String, String]): String = {
    args.foldLeft(macroString) {
      case (m, (k, v)) => m.replaceAll(s"%%${k}%%", v)
    }
  }

  def serializeISO8601Date(date: Date): String = {
    val timezone = TimeZone.getTimeZone("UTC")
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.setTimeZone(timezone)
    dateFormat.format(date)
  }

  def parseISO8601Date(dateStr: String): Date = {
    val timezone = TimeZone.getTimeZone("UTC")
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    dateFormat.setTimeZone(timezone)
    dateFormat.parse(dateStr)
  }

  def readOutputToString(input: InputStream, logOutput: Boolean)(implicit log: Logger): String = {
    val stdInput = new BufferedReader(new InputStreamReader(input))

    var output = ""
    var line = stdInput.readLine
    while (line != null) {
      if (logOutput) {
        log.info(line)
      }
      output += line
      output += '\n'
      line = stdInput.readLine
    }
    input.close()
    output
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
      val files = dir.listFiles()
      if (files.length > 1) {
        val oldestFile = files.sortBy(_.lastModified()).head
        if (!except.contains(oldestFile) && !deleteFileRecursively(oldestFile)) {
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
        if (!except.contains(file) && !file.delete()) {
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
}
