package com.vyulabs.update.utils

import java.io.{BufferedReader, File, InputStream, InputStreamReader}

import com.vyulabs.update.config.CommandConfig
import org.slf4j.Logger

import scala.collection.JavaConverters._
import Utils._
import com.vyulabs.update.utils.ProcessUtils.Logging.Logging

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 31.07.20.
  * Copyright FanDate, Inc.
  */
private class ReaderThread(input: BufferedReader, lineWaitingTimeoutMs: Option[Int],
                   onData: (String) => Unit, onEof: () => Unit, onError: (Exception) => Unit)(implicit log: Logger) extends Thread {

  override def run(): Unit = {
    val line = StringBuilder.newBuilder
    val buffer = Array.apply[Char](1024)
    try {
      var cnt = input.read(buffer)
      while (cnt != -1) {
        line.append(buffer, cnt)
        if (line.endsWith("\n")) {
          onData(line.toString())
          line.clear()
        } else {
          for (lineWaitingTimeoutMs <- lineWaitingTimeoutMs) {
            val expire = System.currentTimeMillis() + lineWaitingTimeoutMs
            var rest = expire - System.currentTimeMillis()
            if (rest > 0) {
              do {
                Thread.sleep(if (rest > 100) 100 else rest)
                rest = expire - System.currentTimeMillis()
              } while (!input.ready() && rest > 0)
            }
            if (!input.ready()) {
              onData(line.toString())
              line.clear()
            }
          }
        }
        cnt = input.read(buffer)
      }
    } catch {
      case e: Exception =>
        onError(e)
    }
    onEof()
  }
}

object ProcessUtils {

  object Logging extends Enumeration {
    type Logging = Value
    val None, Lines, Realtime = Value
  }

  def runProcess(config: CommandConfig, args: Map[String, String], dir: File,
                 logging: Logging)(implicit log: Logger): Boolean = {
    val directory = config.directory match {
      case Some(directory) =>
        if (directory.startsWith("/"))
          new File(directory)
        else
          new File(dir, directory)
      case None => dir
    }
    runProcess(extendMacro(config.command, args),
      config.args.map(extendMacro(_, args)),
      config.env.mapValues(extendMacro(_, args)),
      directory, config.exitCode,
      config.outputMatch.map(extendMacro(_, args)),
      logging)
  }

  def runProcess(command: String, args: Seq[String], env: Map[String, String],
                 dir: File, exitCodeMatch: Option[Int], outputMatch: Option[String],
                 logging: Logging)(implicit log: Logger): Boolean = {
    log.info(s"Executing ${command} with arguments ${args} in directory ${dir}")
    try {
      val builder = new ProcessBuilder((command +: args).toList.asJava)
      builder.redirectErrorStream(true)
      env.foldLeft(builder.environment())((e, entry) => {
        if (entry._2 != null) {
          e.put(entry._1, entry._2)
        } else {
          e.remove(entry._1)
        }
        e
      })
      log.debug(s"Environment: ${builder.environment()}")
      builder.directory(dir)
      val proc = builder.start()

      val output = readOutputToString(proc.getInputStream,
          if (logging == Logging.Realtime) Some(1000) else None, logging != Logging.None).getOrElse {
        log.error("Can't read process output")
        return false
      }
      if (logging == Logging.None && !output.isEmpty) {
        log.debug(s"Output: ${output}")
      }

      val exitCode = proc.waitFor()
      log.debug(s"Exit code: ${exitCode}")

      for (exitCodeMatch <- exitCodeMatch) {
        if (exitCode != exitCodeMatch) {
          log.error(s"Exit code ${exitCode} does not match expected ${exitCodeMatch}")
          return false
        }
      }
      for (outputMatch <- outputMatch) {
        if (!output.matches(outputMatch)) {
          log.error(s"Output does not match ${outputMatch}")
          return false
        }
      }
      log.info("Command executed successfully")
      true
    } catch {
      case ex: Exception =>
        log.error(s"Start process ${command} error: ${ex.getMessage}", ex)
        false
    }
  }

  def readOutput(input: BufferedReader, lineWaitingTimeoutMs: Option[Int],
                 onData: (String) => Unit, onEof: () => Unit, onError: (Exception) => Unit)(implicit log: Logger): Unit = {
    new ReaderThread(input, lineWaitingTimeoutMs, onData, onEof, onError).start()
  }

  def readOutputToString(input: InputStream, readLineTimeoutMs: Option[Int],
                         logOutput: Boolean)(implicit log: Logger): Option[String] = {
    val stdInput = new BufferedReader(new InputStreamReader(input))
    val output = StringBuilder.newBuilder
    val thread = new ReaderThread(stdInput, readLineTimeoutMs,
      data => {
        if (logOutput) {
          log.info(if (data.endsWith("\n")) data.dropRight(1) else data)
        }
        output.append(data)
      },
      () => {},
      ex => {
        log.error("Exception", ex)
        return None
      })
    thread.start()
    thread.join()
    Some(output.toString())
  }
}