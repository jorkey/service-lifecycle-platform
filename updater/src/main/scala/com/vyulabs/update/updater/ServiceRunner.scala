package com.vyulabs.update.updater

import com.vyulabs.update.common.common.Common.InstanceId
import com.vyulabs.update.common.common.Timer
import com.vyulabs.update.common.config.{InstallConfig, RunServiceConfig}
import com.vyulabs.update.common.info.{FaultInfo, ProfiledServiceName}
import com.vyulabs.update.common.logs.LogWriter
import com.vyulabs.update.common.process.{ChildProcess, ProcessMonitor}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import com.vyulabs.update.common.version.{DeveloperDistributionVersion, DeveloperVersion}
import com.vyulabs.update.updater.uploaders.FaultUploader
import org.slf4j.Logger

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceRunner(config: RunServiceConfig, parameters: Map[String, String], directory: File, instanceId: InstanceId,
                    profiledServiceName: ProfiledServiceName, state: ServiceStateController, faultUploader: FaultUploader)
                   (implicit log: Logger, timer: Timer, executionContext: ExecutionContext) {
  private var process = Option.empty[ChildProcess]

  private val maxLogHistoryDirCapacity = 5L * 1000 * 1000 * 1000
  private var lastStartTime = 0L

  def isStarted() : Boolean = process.isDefined

  def startService(): Boolean = {
    synchronized {
      if (isStarted()) {
        state.error("Service process is already started")
        false
      } else {
        val command = Utils.extendMacro(config.command, parameters)
        val arguments = config.args.getOrElse(Seq.empty).map(Utils.extendMacro(_, parameters))
        val env = config.env.getOrElse(Map.empty).mapValues(Utils.extendMacro(_, parameters))
        ChildProcess.start(command, arguments, env, directory).onComplete {
          case Success(process) =>
            this.process = Some(process)
            lastStartTime = System.currentTimeMillis()
            val dateFormat = config.logWriter.dateFormat.map(new SimpleDateFormat(_))
            val logWriter = new LogWriter(new File(state.currentServiceDirectory, config.logWriter.directory),
              config.logWriter.maxFileSizeMB * 1024 * 1024,
              config.logWriter.maxFilesCount,
              config.logWriter.filePrefix,
              (message, exception) => state.error(message, exception))
            process.handleOutput(lines => lines.foreach { case (line, nl) => {
              val formattedLine = dateFormat match {
                case Some(dateFormat) =>
                  s"${dateFormat.format(new Date)} ${line}"
                case None =>
                  line
              }
              logWriter.writeLogLine(formattedLine)
            }})
            for (restartConditions <- config.restartConditions) {
              new ProcessMonitor(process, restartConditions).start()
            }
            process.onTermination().onComplete {
              case Success(exitCode) =>
                this.process = None
                val logTail = logWriter.getLogTail()
                logWriter.close()
                processFault(process, exitCode, logTail)
              case Failure(ex) =>
                this.process = None
                // TODO
            }
          case Failure(ex) =>
            log.error(s"Can't start process ${command}", ex)
        }
        true
      }
    }
  }

  def stopService(): Boolean = {
    process match {
      case Some(process) =>
        try {
          Await.result(process.terminate(), FiniteDuration(30, TimeUnit.SECONDS))
        } catch {
          case _ =>
            false
        }
      case None =>
        true
    }
  }

  def saveLogs(failed: Boolean): Unit = {
    for (currentInstallConfig <- InstallConfig.read(state.currentServiceDirectory)) {
      for (logDirectory <- currentInstallConfig.runService.map(_.logWriter.directory).map(new File(state.currentServiceDirectory, _))) {
        state.info(s"Save log files to history directory")
        if (state.logHistoryDirectory.exists() || state.logHistoryDirectory.mkdir()) {
          val dirName = state.getVersion().getOrElse(DeveloperDistributionVersion("???", DeveloperVersion.empty)).toString + s"-${Utils.serializeISO8601Date(new Date())}" +
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
          state.error(s"Can't make directory ${state.logHistoryDirectory}")
        }
      }
    }
  }

  private def processFault(stoppedProcess: ChildProcess, exitCode: Int, logTail: Queue[String]): Unit = {
    synchronized {
      if (process.contains(stoppedProcess)) {
        state.failure(exitCode)
        saveLogs(true)
        val pattern = config.faultFilesMatch.getOrElse("core")
        val regPattern = Utils.extendMacro(pattern, parameters).r
        val files = directory.listFiles().filter { file =>
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
        val info = FaultInfo(new Date(),  instanceId,
          new java.io.File(".").getCanonicalPath(), profiledServiceName.name, profiledServiceName.profile, state.getState(), logTail)
        faultUploader.addFaultReport(info, reportFilesTmpDir)
        val restartOnFault = config.restartOnFault.getOrElse(true)
        if (restartOnFault) {
          state.info("Try to restart service")
          val period = System.currentTimeMillis() - lastStartTime
          if (period < 1000) {
            Thread.sleep(1000 - period)
          }
          if (!startService()) {
            state.error("Can't restart service")
          }
        } else {
          state.info(s"Service is failed")
        }
      }
    }
  }
}
