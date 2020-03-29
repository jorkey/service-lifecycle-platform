package com.vyulabs.update.updater

import java.io.{File, IOException}

import com.vyulabs.update.config.RestartConditionsConfig
import com.vyulabs.update.utils.UpdateUtils
import org.slf4j.Logger

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 8.05.19.
  * Copyright FanDate, Inc.
  */
class MonitorThread(state: ServiceStateController,
                    process: Process, restartConditions: RestartConditionsConfig)(implicit log: Logger) extends Thread {
  val osName = System.getProperty("os.name")
  val isUnix = osName.startsWith("Linux") || osName.startsWith("Mac")

  override def run(): Unit = {
    try {
      while (process.isAlive) {
        val pid = process.pid()
        for (maxMemorySize <- restartConditions.MaxMemorySize) {
          for (memorySize <- getProcessMemorySize(pid)) {
            if (memorySize > maxMemorySize) {
              state.error(s"Process memory size ${memorySize} > ${maxMemorySize}. Kill it.")
              if (isUnix) {
                Runtime.getRuntime.exec(s"kill -SIGQUIT ${pid}", null, new File("."))
              } else {
                process.destroyForcibly()
              }
              return
            }
          }
        }
        Thread.sleep(5000)
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
        val stdOutput = UpdateUtils.readOutputToString(proc.getInputStream, false)
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
}
