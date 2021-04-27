package com.vyulabs.update.common.process

import com.vyulabs.update.common.common.{Cancelable, Timer}
import com.vyulabs.update.common.config.RestartConditions
import org.slf4j.Logger

import java.io.{File, IOException}
import java.util.IllegalFormatException
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 8.05.19.
  * Copyright FanDate, Inc.
  */
class ProcessMonitor(process: ChildProcess, conditions: RestartConditions)
                    (implicit timer: Timer, log: Logger) {
  private val osName = System.getProperty("os.name")
  private val isUnix = osName.startsWith("Linux") || osName.startsWith("Mac")

  private var cancelable = Option.empty[Cancelable]
  private var lastCpuTime = Option.empty[Long]

  def start() = {
    cancelable match {
      case None =>
        cancelable = Some(timer.schedulePeriodically(checkTask, FiniteDuration(conditions.checkTimeoutMs, TimeUnit.MILLISECONDS)))
      case Some(_) =>
        log.error("Process monitor is already started")
    }
  }

  def stop(): Unit = {
    cancelable.foreach(_.cancel())
    cancelable = None
  }

  private def checkTask() = {
    if (process.isAlive()) {
      for (maxMemorySize <- conditions.maxMemory) {
        val memorySize = getProcessMemorySize(process.getHandle().toHandle).getOrElse(0L) +
          process.getProcessDescendants().foldLeft(0L)((sum, proc) => sum + getProcessMemorySize(proc).getOrElse(0L))
        if (memorySize > maxMemorySize) {
          log.error(s"Process memory size ${memorySize} > ${maxMemorySize}. Kill process group.")
          stop()
          killProcessGroup()
        }
      }
      for (maxCpuPercents <- conditions.maxCpuPercents) {
        for (cpuTime <- getProcessCPU(process.getHandle().toHandle)) {
          for (lastCpuTime <- lastCpuTime) {
            val percents = (cpuTime - lastCpuTime)*100/conditions.checkTimeoutMs
            println(s"${percents}%")
            if (percents >= maxCpuPercents) {
              log.error(s"Process utilize ${percents}% of CPU time. That is >= maximum ${maxCpuPercents}%. Kill process group.")
              stop()
              killProcessGroup()
            }
          }
          lastCpuTime = Some(cpuTime)
        }
      }
    }
  }

  private def getProcessMemorySize(handle: ProcessHandle): Option[Long] = {
    if (isUnix) {
      try {
        val proc = Runtime.getRuntime.exec(s"ps -o vsz= -p ${handle.pid()}", null, new File("."))
        val stdOutput = ProcessUtils.readOutputToString(proc.getInputStream, ProcessUtils.Logging.None).getOrElse {
          log.error(s"Get 'ps' output error")
          return None
        }
        try {
          val value = stdOutput.trim
          if (!value.isEmpty) {
            Some(value.toLong * 1024)
          } else {
            None
          }
        } catch {
          case ex: Exception =>
            log.error(s"Parse 'ps' output error", ex)
            None
        }
      } catch {
        case ex: IOException =>
          log.error("Process creation error", ex)
          None
      }
    } else {
      None
    }
  }

  private def getProcessCPU(handle: ProcessHandle): Option[Long] = {
    if (isUnix) {
      try {
        val proc = Runtime.getRuntime.exec(s"ps -o time= -p ${handle.pid()}", null, new File("."))
        val stdOutput = ProcessUtils.readOutputToString(proc.getInputStream, ProcessUtils.Logging.None).getOrElse {
          log.error(s"Get 'ps' output error")
          return None
        }
        try {
          val value = stdOutput.trim
          if (!value.isEmpty) {
            // Parse format [DD-]hh:mm:ss[.ms]
            val index1 = value.lastIndexOf('.')
            val millis = if (index1 != -1) value.substring(index1+1).toInt*10 else 0
            val index2 = value.lastIndexOf(':', if (index1 != -1) index1-1 else value.length-1)
            if (index2 == -1) {
              throw new IOException()
            }
            val seconds = value.substring(index2+1, if (index1 != -1) index1 else value.length).toInt
            val index3 = value.lastIndexOf(':', index2-1)
            val minutes = value.substring(index3+1, index2).toInt
            var hours, days = 0
            if (index3 != -1) {
              val index4 = value.lastIndexOf('-', index3-1)
              hours = value.substring(index4+1, index3).toInt
              if (index4 != -1) {
                days = value.substring(0, index4).toInt
              }
            }
            val time = FiniteDuration(days, TimeUnit.DAYS).toMillis +
              FiniteDuration(hours, TimeUnit.HOURS).toMillis +
              FiniteDuration(minutes, TimeUnit.MINUTES).toMillis +
              FiniteDuration(seconds, TimeUnit.SECONDS).toMillis +
              millis
            Some(time)
          } else {
            None
          }
        } catch {
          case ex: Exception =>
            log.error(s"Parse 'ps' output error", ex)
            None
        }
      } catch {
        case ex: IOException =>
          log.error("Process creation error", ex)
          None
      }
    } else {
      None
    }

//    val duration = handle.info().totalCpuDuration()
//    if (duration.isPresent) {
//      Some(duration.get().toMillis)
//    } else {
//      None
//    }
  }

  private def killProcessGroup(): Unit = {
    if (conditions.makeCore && isUnix) {
      val command = s"kill -SIGQUIT -- -${process.getHandle().pid()}"
      log.info(s"Execute ${command}")
      Runtime.getRuntime.exec(command, null, new File("."))
    } else {
      process.terminate()
    }
  }
}
