package com.vyulabs.update.common.process

import com.vyulabs.update.common.config.RestartConditionsConfig
import org.slf4j.Logger

import java.io.{File, IOException}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 8.05.19.
  * Copyright FanDate, Inc.
  */
class MonitorThread(process: Process, restartConditions: RestartConditionsConfig)
                   (implicit log: Logger) extends Thread {
  val osName = System.getProperty("os.name")
  val isUnix = osName.startsWith("Linux") || osName.startsWith("Mac")

  override def run(): Unit = {
    try {
      while (process.isAlive) {
        val pid = process.pid()
        for (maxMemorySize <- restartConditions.maxMemory) {
          for (memorySize <- getProcessMemorySize(pid)) {
            if (memorySize > maxMemorySize) {
              log.error(s"Process memory size ${memorySize} > ${maxMemorySize}. Kill it.")
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
        log.error("Monitor service error", e)
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
