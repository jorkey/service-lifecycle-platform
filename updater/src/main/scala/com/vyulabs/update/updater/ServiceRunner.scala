package com.vyulabs.update.updater

import java.io.{BufferedReader, File, InputStreamReader}
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

import com.vyulabs.update.common.Common.VmInstanceId
import com.vyulabs.update.common.{Common, ServiceInstanceName}
import com.vyulabs.update.config.{InstallConfig, RunServiceConfig}
import com.vyulabs.update.distribution.client.ClientDistributionDirectoryClient
import com.vyulabs.update.log.LogWriter
import com.vyulabs.update.updater.uploaders.{FaultReport, FaultUploader, LogUploader}
import com.vyulabs.update.utils.{IOUtils, ProcessUtils, Utils}
import com.vyulabs.update.version.BuildVersion
import org.slf4j.Logger

import scala.collection.JavaConverters._
import scala.collection.immutable.Queue

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ServiceRunner(instanceId: VmInstanceId, serviceInstanceName: ServiceInstanceName,
                    state: ServiceStateController, clientDirectory: ClientDistributionDirectoryClient,
                    faultUploader: FaultUploader)(implicit log: Logger) {
    private case class ProcessParameters(config: RunServiceConfig, args: Map[String, String], directory: File)

  private var processParameters = Option.empty[ProcessParameters]
  private var process = Option.empty[Process]
  private var lastStartTime = 0L

  private val maxLogHistoryDirCapacity = 5 * 1000 * 1000 * 1000

  private val processTerminateTimeoutMs = 5000

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
              var descendantProcesses = getProcessDescendants(p.toHandle)
              state.info(s"Stop service, process ${p.pid()}, descendant processes ${descendantProcesses.map(_.pid())}")
              p.destroy()
              if (!p.waitFor(processTerminateTimeoutMs, TimeUnit.MILLISECONDS)) {
                state.error(s"Service process ${p.pid()} is not terminated normally during ${processTerminateTimeoutMs}ms - destroy it forcibly")
                p.destroyForcibly()
              }
              val status = p.waitFor()
              state.info(s"Service process ${p.pid()} is terminated with status ${status}.")
              descendantProcesses = descendantProcesses.filter(_.isAlive)
              if (!descendantProcesses.isEmpty) {
                state.info(s"Destroy remaining descendant processes ${descendantProcesses.map(_.pid())}")
                descendantProcesses.foreach(_.destroy())
                val stopBeginTime = System.currentTimeMillis()
                do {
                  Thread.sleep(1000)
                  descendantProcesses = descendantProcesses.filter(_.isAlive)
                } while (!descendantProcesses.isEmpty && System.currentTimeMillis() - stopBeginTime < processTerminateTimeoutMs)
                if (!descendantProcesses.isEmpty) {
                  state.error(s"Service descendant processes ${descendantProcesses.map(_.pid())} are not terminated normally during ${processTerminateTimeoutMs}ms seconds - destroy them forcibly")
                  descendantProcesses.foreach(_.destroyForcibly())
                }
              }
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

  def getProcessDescendants(process: ProcessHandle): Seq[ProcessHandle] = {
    var processes = Seq.empty[ProcessHandle]
    process.children().forEach { process =>
      processes :+= process
      processes ++= getProcessDescendants(process)
    }
    processes
  }

  def processFault(stoppedProcess: Process, exitCode: Int, logTail: Queue[String]): Unit = {
    synchronized {
      if (process.contains(stoppedProcess)) {
        state.failure(exitCode)
        saveLogs(true)
        val reportFilesTmpDir = processParameters match {
          case Some(params) =>
            val pattern = params.config.faultFilesMatch.getOrElse("core")
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
        faultUploader.addFaultReport(FaultReport(instanceId, state.getState(), reportFilesTmpDir, logTail))
        val restartOnFault = processParameters.map(_.config.restartOnFault).getOrElse(false)
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
      for (logDirectory <- currentInstallConfig.runService.map(_.logWriter.directory).map(new File(state.currentServiceDirectory, _))) {
        state.info(s"Save log files to history directory")
        if (state.logHistoryDirectory.exists() || state.logHistoryDirectory.mkdir()) {
          val dirName = state.getVersion().getOrElse(BuildVersion.empty).toString + s"-${Utils.serializeISO8601Date(new Date())}" +
            (if (failed) "-failed" else "")
          val saveDir = new File(state.logHistoryDirectory, s"${dirName}.log")
          if (!saveDir.exists() || IOUtils.deleteFileRecursively(saveDir)) {
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
          IOUtils.maybeFreeSpace(state.logHistoryDirectory, maxLogHistoryDirCapacity, Set.empty)
        } else {
          state.error(s"Can't make directory ${state.logHistoryDirectory}")
        }
      }
    }
  }

  private def startService(): Boolean = {
    try {
      for (params <- processParameters) {
        val command = Utils.extendMacro(params.config.command, params.args)
        val arguments = params.config.args.map(Utils.extendMacro(_, params.args))
        val builder = new ProcessBuilder((command +: arguments).toList.asJava)
        builder.redirectErrorStream(true)
        var macroArgs = Map.empty[String, String]
        macroArgs += ("PATH" -> System.getenv("PATH"))
        params.config.env.foldLeft(builder.environment())((e, entry) => {
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
        val logWriter = new LogWriter(new File(state.currentServiceDirectory, params.config.logWriter.directory),
          params.config.logWriter.maxFileSizeMB * 1024 * 1024,
          params.config.logWriter.maxFilesCount,
          params.config.logWriter.filePrefix,
          (message, exception) => state.error(message, exception))
        val logUploader = params.config.logUploader match {
          case Some(logsUploaderConfig) =>
            val uploader = new LogUploader(instanceId, serviceInstanceName, logsUploaderConfig, clientDirectory)
            uploader.start()
            Some(uploader)
          case None =>
            None
        }
        val dateFormat = params.config.logWriter.dateFormat.map(new SimpleDateFormat(_))
        val input = new BufferedReader(new InputStreamReader(process.getInputStream))
        ProcessUtils.readOutputLines(input, None,
          (line, nl) => {
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
          },
          exception => {
            if (process.isAlive) {
              state.error(s"Read service output error ${exception.getMessage}")
            }
          })
        for (restartConditions <- params.config.restartConditions) {
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
