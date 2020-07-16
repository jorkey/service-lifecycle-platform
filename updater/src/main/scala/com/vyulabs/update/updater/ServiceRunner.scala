package com.vyulabs.update.updater

import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import com.vyulabs.update.common.Common.InstanceId
import com.vyulabs.update.common.ServiceInstanceName
import com.vyulabs.update.config.{InstallConfig, RunServiceConfig}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.log.LogWriter
import com.vyulabs.update.updater.uploaders.{FaultReport, FaultUploader, LogUploader}
import com.vyulabs.update.utils.Utils
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import scala.collection.JavaConverters._
import scala.collection.immutable.Queue

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceRunner(instanceId: InstanceId, serviceInstanceName: ServiceInstanceName,
                    state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient,
                    faultUploader: FaultUploader)(implicit log: Logger) {
    private case class ProcessParameters(config: RunServiceConfig, args: Map[String, String], directory: File)

  private var processParameters = Option.empty[ProcessParameters]
  private var process = Option.empty[Process]
  private var lastStartTime = 0L

  private val maxLogHistoryDirCapacity = 5 * 1000 * 1000 * 1000

  def isStarted() : Boolean = process.isDefined

  def startService(config: RunServiceConfig, args: Map[String, String], directory: File): Boolean = {
    synchronized {
      processParameters match {
        case Some(_) =>
          state.error("Service process is already started")
          false
        case None =>
          processParameters = Some(ProcessParameters(config, args, directory))
          startService()
      }
    }
  }

  def stopService(): Boolean = {
    synchronized {
      try {
        process match {
          case Some(p) =>
            val terminated = try {
              state.info(s"Stop service, process ${p.pid()}")
              p.destroy()
              if (!p.waitFor(5, TimeUnit.SECONDS)) {
                state.error(s"Service process ${p.pid()} is not terminated normally during 5 seconds - destroy it forcibly")
                p.destroyForcibly()
              }
              val status = p.waitFor()
              state.info(s"Service process ${p.pid()} is terminated with status ${status}.")
              true
            } catch {
              case e: Exception =>
                state.error(s"Stop process ${p.pid()} error", e)
                false
            }
            if (terminated) {
              processParameters = None
              process = None
              true
            } else {
              false
            }
          case None =>
            true
        }
      } catch {
        case e: Exception =>
          state.error("Stop process error", e)
          false
      }
    }
  }

  def processFault(stoppedProcess: Process, exitCode: Int, logTail: Queue[String]): Unit = {
    synchronized {
      if (process.contains(stoppedProcess)) {
        state.failure(exitCode)
        saveLogs(true)
        val reportFilesTmpDir = processParameters match {
          case Some(params) =>
            params.config.FaultFilesMatch match {
              case Some(pattern) =>
                val regPattern = Utils.extendMacro(pattern, params.args).r
                val files = params.directory.listFiles().filter { file =>
                  file.getName match {
                    case regPattern() => true
                    case _ => false
                  }
                }
                if (!files.isEmpty) {
                  val reportTmpDir = Files.createTempDirectory(s"${serviceInstanceName}-fault-").toFile
                  for (file <- files) {
                    val tmpFile = new File(reportTmpDir, file.getName)
                    if (!file.renameTo(tmpFile)) {
                      log.error(s"Can't rename ${file} to ${tmpFile}")
                    }
                  }
                  Some(reportTmpDir)
                } else {
                  None
                }
              case None =>
                None
            }
          case None =>
            None
        }
        faultUploader.addFaultReport(FaultReport(instanceId, state.getState(), reportFilesTmpDir, logTail))
        val restartOnFault = processParameters.map(_.config.RestartOnFault).getOrElse(false)
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

  def saveLogs(failed: Boolean): Unit = {
    for (currentInstallConfig <- InstallConfig(state.currentServiceDirectory)) {
      for (logDirectory <- currentInstallConfig.RunService.map(_.LogWriter.Directory).map(new File(state.currentServiceDirectory, _))) {
        state.info(s"Save log files to history directory")
        if (state.logHistoryDirectory.exists() || state.logHistoryDirectory.mkdir()) {
          val dirName = state.getVersion().getOrElse(BuildVersion.empty).toString + s"-${Utils.serializeISO8601Date(new Date())}" +
            (if (failed) "-failed" else "")
          val saveDir = new File(state.logHistoryDirectory, s"${dirName}.log")
          if (!saveDir.exists() || Utils.deleteFileRecursively(saveDir)) {
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
          Utils.maybeFreeSpace(state.logHistoryDirectory, maxLogHistoryDirCapacity, Set.empty)
        } else {
          state.error(s"Can't make directory ${state.logHistoryDirectory}")
        }
      }
    }
  }

  private def startService(): Boolean = {
    try {
      for (params <- processParameters) {
        val command = Utils.extendMacro(params.config.Command, params.args)
        val arguments = params.config.Arguments.map(Utils.extendMacro(_, params.args))
        val builder = new ProcessBuilder((command +: arguments).toList.asJava)
        builder.redirectErrorStream(true)
        var macroArgs = Map.empty[String, String]
        macroArgs += ("PATH" -> System.getenv("PATH"))
        params.config.Env.foldLeft(builder.environment())((e, entry) => {
          if (entry._2 != null) {
            e.put(entry._1, Utils.extendMacro(entry._2, macroArgs))
          } else {
            e.remove(entry._1)
          }
          e
        })
        builder.directory(params.directory)
        state.info(s"Start command ${command} with arguments ${arguments} in directory ${params.directory}")
        log.debug(s"Environment: ${builder.environment().asScala}")
        lastStartTime = System.currentTimeMillis()
        val process = builder.start()
        this.process = Some(process)
        log.debug(s"Started process ${process.pid()}")
        val logWriter = new LogWriter(new File(state.currentServiceDirectory, params.config.LogWriter.Directory),
          params.config.LogWriter.MaxFileSizeMB * 1024 * 1024,
          params.config.LogWriter.MaxFilesCount,
          params.config.LogWriter.FilePrefix,
          (message, exception) => state.error(message, exception))
        val logUploader = params.config.LogUploader match {
          case Some(logsUploaderConfig) =>
            val uploader = new LogUploader(instanceId, serviceInstanceName, logsUploaderConfig, clientDirectory)
            uploader.start()
            Some(uploader)
          case None =>
            None
        }
        val dateFormat = params.config.LogWriter.DateFormat.map(new SimpleDateFormat(_))
        new ReaderThread(state, process,
          line => {
            val formattedLine = dateFormat match {
              case Some(dateFormat) =>
                s"${dateFormat.format(new Date)} ${line}"
              case None =>
                line
            }
            logWriter.writeLogLine(formattedLine)
            logUploader.foreach(_.writeLogLine(formattedLine))
          },
          () => {
            val logTail = logWriter.getLogTail()
            logWriter.close()
            logUploader.foreach(_.close())
            val exitCode = process.waitFor()
            log.debug(s"Service ${serviceInstanceName} is terminated with code ${exitCode}")
            processFault(process, exitCode, logTail)
          }).start()
        for (restartConditions <- params.config.RestartConditions) {
          new MonitorThread(state, process, restartConditions).start()
        }
      }
      true
    } catch {
      case e: Exception =>
        state.error("Start service error", e)
        false
    }
  }
}
