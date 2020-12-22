package com.vyulabs.update.common.process

import com.vyulabs.update.common.common.{Cancelable, Timer}
import com.vyulabs.update.common.config.RestartConditions
import org.slf4j.Logger

import java.io.{File, IOException}
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
      val pid = process.getHandle().pid()
      for (maxMemorySize <- conditions.maxMemory) {
        val memorySize = process.getProcessDescendants().foldLeft(0L)((sum, proc) => sum + getProcessMemorySize(proc.pid).getOrElse(0L))
        if (memorySize > maxMemorySize) {
          stop()
          log.error(s"Process memory size ${memorySize} > ${maxMemorySize}. Kill process group.")
          if (conditions.makeCore && isUnix) {
            val command = s"kill -SIGQUIT -- -${pid}"
            log.info(s"Execute ${command}")
            Runtime.getRuntime.exec(command, null, new File("."))
          } else {
            process.terminate()
          }
        }
      }
    }
  }

  private def getProcessMemorySize(pid: Long): Option[Long] = {
    if (isUnix) {
      try {
        val proc = Runtime.getRuntime.exec(s"ps -o vsz= -p ${pid}", null, new File("."))
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
}
