package com.vyulabs.update.updater

import com.vyulabs.update.config.RestartConditionsConfig
import com.vyulabs.update.utils.ProcessUtils

import java.io.{File, IOException}
import org.slf4j.Logger

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 8.05.19.
  * Copyright FanDate, Inc.
  */
class MonitorThread(state: ServiceStateController,
                    process: Process, restartConditions: RestartConditionsConfig)(implicit log: Logger) extends Thread {
  private val osName = System.getProperty("os.name")
  private val isUnix = osName.startsWith("Linux") || osName.startsWith("Mac")

  private val checkTimeoutMs = 5000

  private var lastCpuMeasure = Option.empty[Long]
  private var lastCpuTime = Option.empty[Long]

  override def run(): Unit = {
    try {
      while (process.isAlive) {
        val pid = process.pid()
        for (maxCpu <- restartConditions.maxCpu) {
          if (lastCpuMeasure.isEmpty || System.currentTimeMillis() >= lastCpuMeasure.get + maxCpu.durationSec*1000) {
            for (cpuTime <- getProcessCPU(process.toHandle)) {
              for (lastCpuTime <- lastCpuTime) {
                val percents = (cpuTime - lastCpuTime)*100/(maxCpu.durationSec*1000)
                if (percents >= maxCpu.percents) {
                  log.error(s"Process ${process.toHandle.pid()} CPU utilize ${percents}% >= ${maxCpu.percents}%. Kill process group.")
                  killProcessGroup()
                } else {
                  log.debug(s"Process ${process.toHandle.pid()} CPU utilize ${percents}%")
                }
              }
              lastCpuTime = Some(cpuTime)
            }
            lastCpuMeasure = Some(System.currentTimeMillis())
          }
        }
        for (maxMemorySize <- restartConditions.maxMemory) {
          for (memorySize <- getProcessMemorySize(pid)) {
            if (memorySize > maxMemorySize) {
              state.error(s"Process memory size ${memorySize} > ${maxMemorySize}. Kill it.")
              killProcessGroup()
              return
            }
          }
        }
        Thread.sleep(checkTimeoutMs)
      }
    } catch {
      case e: Exception =>
        state.error("Monitor service error", e)
    }
  }

  private def getProcessMemorySize(pid: Long): Option[Long] = {
    if (isUnix) {
      try {
        val proc = Runtime.getRuntime.exec(s"ps -o vsz= -p ${pid}", null, new File("."))
        val stdOutput = ProcessUtils.readOutputToString(proc.getInputStream, ProcessUtils.Logging.None).getOrElse {
          state.error(s"Get 'ps' output error")
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
            state.error(s"Parse 'ps' output error", ex)
            None
        }
      } catch {
        case ex: IOException =>
          state.error("Process creation error", ex)
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
  }

  private def killProcessGroup(): Unit = {
    if (isUnix) {
      Runtime.getRuntime.exec(s"kill -SIGQUIT ${process.pid()}", null, new File("."))
    } else {
      process.destroyForcibly()
    }
  }
}