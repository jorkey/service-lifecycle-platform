package com.vyulabs.update.common.process

import org.slf4j.Logger

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future, Promise}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 17.04.19.
  * Copyright FanDate, Inc.
  */
class ChildProcess(process: Process)
                  (implicit executionContext: ExecutionContext, log: Logger) {
  private val processTerminateTimeoutMs = 5000
  private val exitCode = Promise[Int]

  def isStarted(): Boolean = process.isAlive

  def getExitCode(): Future[Int] = exitCode.future

  def getProcess() = process

  def handleOutput(onOutputLine: Option[(String, Boolean) => Unit]): Unit = {
    val input = new BufferedReader(new InputStreamReader(process.getInputStream))
    new LinesReaderThread(input, None,
      (_, _) => { for (onOutputLine <- onOutputLine) { onOutputLine(_, _) } },
      () => {
        val status = process.waitFor()
        log.info(s"Process ${process.pid()} is terminated with status ${status}.")
        exitCode.success(status)
      },
      exception => { if (process.isAlive) log.error(s"Read process output error ${exception.toString}") }
    ).start()
  }

  def stop(): Future[Boolean] = {
    Future {
      var descendantProcesses = getProcessDescendants(process.toHandle)
      log.info(s"Stop process ${process.pid()}, descendant processes ${descendantProcesses.map(_.pid())}")
      process.destroy()
      if (!process.waitFor(processTerminateTimeoutMs, TimeUnit.MILLISECONDS)) {
        log.error(s"Process ${process.pid()} is not terminated normally during ${processTerminateTimeoutMs}ms - destroy it forcibly")
        process.destroyForcibly()
      }
      val status = process.waitFor()
      log.info(s"Process ${process.pid()} is terminated with status ${status}.")
      exitCode.success(status)
      descendantProcesses = descendantProcesses.filter(_.isAlive)
      if (!descendantProcesses.isEmpty) {
        log.info(s"Destroy remaining descendant processes ${descendantProcesses.map(_.pid())}")
        descendantProcesses.foreach(_.destroy())
        val stopBeginTime = System.currentTimeMillis()
        do {
          Thread.sleep(1000)
          descendantProcesses = descendantProcesses.filter(_.isAlive)
        } while (!descendantProcesses.isEmpty && System.currentTimeMillis() - stopBeginTime < processTerminateTimeoutMs)
        if (!descendantProcesses.isEmpty) {
          log.error(s"Descendant processes ${descendantProcesses.map(_.pid())} are not terminated normally during ${processTerminateTimeoutMs}ms seconds - destroy them forcibly")
          descendantProcesses.foreach(_.destroyForcibly())
        }
      }
      true
    }
  }

  private def getProcessDescendants(process: ProcessHandle): Seq[ProcessHandle] = {
    var processes = Seq.empty[ProcessHandle]
    process.children().forEach { process =>
      processes :+= process
      processes ++= getProcessDescendants(process)
    }
    processes
  }
}

object ChildProcess {
  def start(command: String, args: Seq[String], env: Map[String, String], directory: File)
           (implicit executionContext: ExecutionContext, log: Logger): Option[ChildProcess] = {
    try {
      val builder = new ProcessBuilder(command).command(args.asJava)
      env.foldLeft(builder.environment())((e, entry) => {
        if (entry._2 != null) { e.put(entry._1, entry._2) }
        else { e.remove(entry._1) }
        e
      })
      builder.redirectErrorStream(true)
      builder.directory(directory)
      log.info(s"Start command ${command} with arguments ${args} in directory ${directory}")
      log.debug(s"Environment: ${builder.environment().asScala}")
      val process = builder.start()
      log.debug(s"Started process ${process.pid()}")
      Some(new ChildProcess(process))
    } catch {
      case e: Exception =>
        log.error("Start process error", e)
        None
    }
  }
}