package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.config.RunServiceConfig
import com.vyulabs.update.common.info.{FaultInfo, LogLine, ServiceNameWithRole}
import com.vyulabs.update.common.logger.{LogBuffer, LogReceiver}
import com.vyulabs.update.common.logs.{LogFormat, LogWriter}
import com.vyulabs.update.common.process.{ChildProcess, ProcessMonitor}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.{Build, DeveloperDistributionVersion}
import com.vyulabs.update.updater.uploaders.FaultUploader
import org.slf4j.Logger

import java.io.{File, IOException}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import scala.collection.immutable.Queue
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceRunner(config: RunServiceConfig, parameters: Map[String, String], instance: InstanceId,
                    profiledServiceName: ServiceNameWithRole, state: ServiceStateController,
                    logUploader: Option[LogReceiver], faultUploader: FaultUploader)
                   (implicit log: Logger, timer: Timer, executionContext: ExecutionContext) { self =>
  private val maxLogHistoryDirCapacity = 5L * 1000 * 1000 * 1000

  private var currentProcess = Option.empty[ChildProcess]
  private var stoppingProcesses = Set.empty[ChildProcess]
  private var lastStartTime = 0L

  def startService(): Boolean = {
    self.synchronized {
      log.info("Start service")
      if (currentProcess.isDefined) {
        log.error("Service is already started")
        false
      } else {
        val getMacroContent = (path: String) =>
          IoUtils.readFileToBytes(new File(state.currentServiceDirectory, path)).getOrElse(throw new IOException())
        val command = Utils.extendMacro(config.command, parameters, getMacroContent)
        val arguments = config.args.getOrElse(Seq.empty).map(Utils.extendMacro(_, parameters, getMacroContent))
        val env = config.env.getOrElse(Map.empty).mapValues(Utils.extendMacro(_, parameters, getMacroContent))
        val logWriter = config.writeLogs.map { writeLogsConfig =>
          new LogWriter(new File(state.currentServiceDirectory, writeLogsConfig.directory),
            writeLogsConfig.maxFileSizeMB * 1024 * 1024,
            writeLogsConfig.maxFilesCount,
            writeLogsConfig.filePrefix,
            (message, exception) => log.error(message, exception))
        }
        val logUploaderBuffer = logUploader.map { logUploader =>
          new LogBuffer(s"Service ${profiledServiceName}", "SERVICE",
            logUploader, 100, 1000)
        }
        val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        val onOutput = logWriter.map { writeLogs =>
          (lines: Seq[(String, Boolean)]) => {
            lines.foreach {
              case (line, nl) =>
                val logLine = LogFormat.parse(line)
                writeLogs.writeLogLine(logLine)
                logUploaderBuffer.foreach(uploaderBuffer => {
                  uploaderBuffer.append(logLine)
                })
            }
            ()
          }
        }.getOrElse((_: Seq[(String, Boolean)]) => ())
        val process = try {
          Await.result(ChildProcess.start(command, arguments, env, state.currentServiceDirectory), Duration.Inf)
        } catch {
          case e: Exception =>
            logUploaderBuffer.foreach(_.append(LogLine(new Date(), "ERROR", "SERVICE",
              s"Can't start process ${e.getMessage}", None)))
            log.error("Can't start process", e)
            return false
        }
        val onExit = (exitCode: Int) => {
          processExit(process, exitCode, logWriter, logUploaderBuffer)
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
    self.synchronized {
      log.info(s"Stop service")
      val result = currentProcess match {
        case Some(process) =>
          stoppingProcesses += process
          try {
            log.info(s"Terminate ${process.getHandle().pid()}")
            val result = Await.result(process.terminate(), Duration.Inf)
            if (result) {
              log.info(s"Wait for process ${process.getHandle().pid()} termination")
              var completed = false
              do {
                self.wait(100)
                if (process.onTermination().isCompleted) {
                  log.info(s"Process ${process.getHandle().pid()} stopping is completed")
                  completed = true
                }
              } while (!completed)
            }
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
    self.synchronized {
      currentProcess.isDefined
    }
  }

  def saveLogs(failed: Boolean): Unit = {
    self.synchronized {
      for (logDirectory <- config.writeLogs.map(_.directory).map(new File(state.currentServiceDirectory, _))) {
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

  private def processExit(process: ChildProcess, exitCode: Int,
                          logWriter: Option[LogWriter], logUploaderBuffer: Option[LogBuffer]): Unit = {
    self.synchronized {
      if (currentProcess.contains(process)) {
        log.info(s"Process exit of service process")
        val logTail = logWriter.map { logWriter =>
          val logTail = logWriter.getLogTail()
          logWriter.close()
          logTail
        }.getOrElse(Queue.empty)
        currentProcess = None
        state.failure(exitCode)
        saveLogs(true)
        if (!stoppingProcesses.contains(process)) {
          generateFaultReport(logTail)
          val restartOnFault = config.restartOnFault.getOrElse(true)
          if (restartOnFault) {
            log.info("Try to restart service")
            val period = System.currentTimeMillis() - lastStartTime
            if (period < 1000) {
              Thread.sleep(1000 - period)
            }
            if (!startService()) {
              log.error("Can't restart service")
            }
          }
        } else {
          stoppingProcesses -= process
          log.info(s"Service is terminated")
        }
      } else {
        log.error("Not current process is terminated")
        stoppingProcesses -= process
      }
      logUploaderBuffer.foreach(_.stop(Some(exitCode == 0), None))
      self.notifyAll()
    }
  }

  private def generateFaultReport(logTail: Queue[LogLine]): Unit = {
    val pattern = config.faultFilesMatch.getOrElse("core")
    def getMacroContent = (path: String) =>
      IoUtils.readFileToBytes(new File(state.currentServiceDirectory, path)).getOrElse {
        throw new IOException("Get macro content error") }
    val regPattern = Utils.extendMacro(pattern, parameters, getMacroContent).r
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
    val info = FaultInfo(new Date(), instance, profiledServiceName.name, profiledServiceName.role,
      new java.io.File(".").getCanonicalPath(), state.getState(), logTail)
    faultUploader.addFaultReport(info, reportFilesTmpDir)
  }
}
