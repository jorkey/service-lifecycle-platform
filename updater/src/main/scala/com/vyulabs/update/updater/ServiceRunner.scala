package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.config.RunServiceConfig
import com.vyulabs.update.common.info.{FaultInfo, LogLine, ProfiledServiceName}
import com.vyulabs.update.common.logger.{LogBuffer, LogReceiver}
import com.vyulabs.update.common.logs.{LogFormat, LogWriter}
import com.vyulabs.update.common.process.{ChildProcess, ProcessMonitor}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.{Build, DeveloperDistributionVersion}
import com.vyulabs.update.updater.uploaders.FaultUploader
import org.slf4j.Logger

import java.io.File
import java.nio.file.Files
import java.text.{ParseException, SimpleDateFormat}
import java.util.concurrent.TimeUnit
import java.util.{Date, TimeZone}
import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceRunner(config: RunServiceConfig, parameters: Map[String, String], instance: InstanceId,
                    profiledServiceName: ProfiledServiceName, state: ServiceStateController,
                    logUploader: Option[LogReceiver], faultUploader: FaultUploader)
                   (implicit log: Logger, timer: Timer, executionContext: ExecutionContext) {
  private val maxLogHistoryDirCapacity = 5L * 1000 * 1000 * 1000

  private var currentProcess = Option.empty[ChildProcess]
  private var stopping = false
  private var lastStartTime = 0L

  private val logUnitName = "SERVICE"

  def startService(): Boolean = {
    synchronized {
      log.info("Start service")
      if (currentProcess.isDefined) {
        log.error("Service is already started")
        false
      } else {
        stopping = false
        val command = Utils.extendMacro(config.command, parameters)
        val arguments = config.args.getOrElse(Seq.empty).map(Utils.extendMacro(_, parameters))
        val env = config.env.getOrElse(Map.empty).mapValues(Utils.extendMacro(_, parameters))
        val logWriter = config.logWriter.map { logWriterConfig =>
          new LogWriter(new File(state.currentServiceDirectory, logWriterConfig.directory),
            logWriterConfig.maxFileSizeMB * 1024 * 1024,
            logWriterConfig.maxFilesCount,
            logWriterConfig.filePrefix,
            (message, exception) => log.error(message, exception))
        }
        val logUploaderBuffer = logUploader.map { logUploader =>
          new LogBuffer(s"Service ${profiledServiceName}", logUnitName,
            logUploader, 100, 1000)
        }
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        val formattedLogRegex = "(.[^ ]* .[^ ]*) (.[^ ]*) (.[^ ]*) (.*)".r
        val onOutput = logWriter.map { logWriter =>
          (lines: Seq[(String, Boolean)]) => {
            lines.foreach {
              case (line, nl) =>
                val logLine = LogFormat.parse(line, logUnitName)
                logWriter.writeLogLine(logLine)
                logUploaderBuffer.foreach(uploaderBuffer => {
                  uploaderBuffer.append(logLine)
                })
            }
            ()
          }
        }.getOrElse((_: Seq[(String, Boolean)]) => ())
        val onExit = (exitCode: Int) => {
          val logTail = logWriter.map { logWriter =>
            val logTail = logWriter.getLogTail()
            logWriter.close()
            logTail
          }.getOrElse(Queue.empty)
          log.info(s"Process exit of service process")
          processExit(exitCode, logTail)
          logUploaderBuffer.foreach(_.stop(Some(exitCode==0), None))
        }
        val process = try {
          Await.result(ChildProcess.start(command, arguments, env, state.currentServiceDirectory), FiniteDuration(10, TimeUnit.SECONDS))
        } catch {
          case e: Exception =>
            logUploaderBuffer.foreach(_.append(LogLine(new Date(), "ERROR", logUnitName, s"Can't start process ${e.getMessage}", None)))
            log.error("Can't start process", e)
            return false
        }
        process.readOutput(onOutput, onExit)
        logUploaderBuffer.foreach(_.start())
        currentProcess = Some(process)
        lastStartTime = System.currentTimeMillis()
        for (restartConditions <- config.restartConditions) {
          new ProcessMonitor(process, restartConditions).start()
        }
        true
      }
    }
  }

  def stopService(): Boolean = {
    synchronized {
      log.info("Stop service")
      stopping = true
      val result = currentProcess match {
        case Some(process) =>
          try {
            val result = Await.result(process.terminate(), FiniteDuration(30, TimeUnit.SECONDS))
            currentProcess = None
            result
          } catch {
            case _: Exception =>
              false
          }
        case None =>
          true
      }
      result
    }
  }

  def isServiceRunning(): Boolean = {
    synchronized {
      currentProcess.isDefined
    }
  }

  def saveLogs(failed: Boolean): Unit = {
    synchronized {
      for (logDirectory <- config.logWriter.map(_.directory).map(new File(state.currentServiceDirectory, _))) {
        log.info(s"Save log files to history directory")
        if (state.logHistoryDirectory.exists() || state.logHistoryDirectory.mkdir()) {
          val dirName = state.getVersion().getOrElse(DeveloperDistributionVersion("???", Build.empty)).toString + s"-${Utils.serializeISO8601Date(new Date())}" +
            (if (failed) "-failed" else "")
          val saveDir = new File(state.logHistoryDirectory, s"${dirName}.log")
          if (!saveDir.exists() || IoUtils.deleteFileRecursively(saveDir)) {
            if (logDirectory.exists()) {
              if (!logDirectory.renameTo(saveDir)) {
                log.error(s"Can't rename ${logDirectory} to ${saveDir}")
              }
            } else {
              log.error(s"Log directory ${logDirectory} not exist")
            }
          } else {
            log.error(s"Can't delete ${saveDir}")
          }
          IoUtils.maybeFreeSpace(state.logHistoryDirectory, maxLogHistoryDirCapacity, Set.empty)
        } else {
          log.error(s"Can't make directory ${state.logHistoryDirectory}")
        }
      }
    }
  }

  private def processExit(exitCode: Int, logTail: Queue[LogLine]): Unit = {
    synchronized {
      state.failure(exitCode)
      saveLogs(true)
      val pattern = config.faultFilesMatch.getOrElse("core")
      val regPattern = Utils.extendMacro(pattern, parameters).r
      val files = state.currentServiceDirectory.listFiles().filter { file =>
        file.getName match {
          case regPattern() => true
          case _ => false
        }
      }
      val reportFilesTmpDir = if (!files.isEmpty) {
        val reportTmpDir = Files.createTempDirectory(s"${profiledServiceName}-fault-").toFile
        for (file <- files) {
          val tmpFile = new File(reportTmpDir, file.getName)
          if (!file.renameTo(tmpFile)) log.error(s"Can't rename ${file} to ${tmpFile}")
        }
        Some(reportTmpDir)
      } else {
        None
      }
      val info = FaultInfo(new Date(), instance, profiledServiceName.name, new java.io.File(".").getCanonicalPath(), profiledServiceName.profile, state.getState(), logTail)
      faultUploader.addFaultReport(info, reportFilesTmpDir)
      val restartOnFault = config.restartOnFault.getOrElse(true)
      if (restartOnFault && !stopping) {
        log.info("Try to restart service")
        val period = System.currentTimeMillis() - lastStartTime
        if (period < 1000) {
          Thread.sleep(1000 - period)
        }
        if (!stopService() || !startService()) {
          log.error("Can't restart service")
        }
      } else {
        log.info(s"Service is terminated")
      }
      currentProcess = None
      stopping = false
    }
  }
}
