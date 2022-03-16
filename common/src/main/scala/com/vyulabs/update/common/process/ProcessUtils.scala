package com.vyulabs.update.common.process

import com.vyulabs.update.common.config.CommandConfig
import com.vyulabs.update.common.process.ProcessUtils.Logging.Logging
import com.vyulabs.update.common.utils.IoUtils
import com.vyulabs.update.common.utils.Utils.extendMacro
import org.slf4j.Logger

import java.io.{BufferedReader, File, IOException, InputStreamReader}
import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 31.07.20.
  * Copyright FanDate, Inc.
  */
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
    def getMacroContent = (path: String) => {
      IoUtils.readFileToBytes(new File(directory, path)).getOrElse { throw new IOException("Get macro content error") }
    }
    runProcess(extendMacro(config.command, args, getMacroContent),
      config.args.getOrElse(Seq.empty).map(extendMacro(_, args, getMacroContent)),
      config.env.getOrElse(Map.empty).mapValues(extendMacro(_, args, getMacroContent)),
      directory, config.exitCode,
      config.outputMatch.map(extendMacro(_, args, getMacroContent)),
      logging)
  }

  def runProcess(command: String, args: Seq[String], env: Map[String, String],
                 dir: File, exitCodeMatch: Option[Int], outputMatch: Option[String],
                 logging: Logging)(implicit log: Logger): Boolean = {
    log.info(s"Executing ${command} with arguments ${args} in directory ${dir}")
    var processToTerminate = Option.empty[Process]
    val terminateThread = new Thread() {
      override def run(): Unit = {
        processToTerminate.foreach( proc => {
          log.info(s"Program is terminating - kill child process ${proc.pid()}")
          try {
            Await.result(new ChildProcess(proc).terminate(), Duration.Inf)
          } catch {
            case e: Exception =>
          }
        })
    }}
    Runtime.getRuntime.addShutdownHook(terminateThread)
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
      processToTerminate = Some(proc)
      val output = readOutputToString(proc, logging).getOrElse {
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
        log.error(s"Start process ${command} error: ${ex.toString}", ex)
        false
    } finally {
      try { Runtime.getRuntime.removeShutdownHook(terminateThread) } catch { case _: Exception => {} }
    }
  }

  def readOutputToString(process: Process, logging: Logging)
                        (implicit log: Logger): Option[String] = {
    val stdInput = new BufferedReader(new InputStreamReader(process.getInputStream))
    val output = StringBuilder.newBuilder
    val thread = new OutputReaderThread(stdInput, if (logging == Logging.Realtime) Some(1000) else None,
      () => !process.isAlive,
      lines => {
        lines.foreach { case (data, nl) =>
          if (logging != Logging.None) {
            log.info(data)
          }
          output.append(data)
          if (nl) {
            output.append('\n')
          }
        }
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