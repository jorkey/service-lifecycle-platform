package com.vyulabs.update.process

import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.config.{MaxCpu, RestartConditions}
import com.vyulabs.update.common.process.{ChildProcess, ProcessMonitor}
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import org.scalatest.{FlatSpec, Matchers}

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 22.12.20.
  * Copyright FanDate, Inc.
  */
class ProcessMonitorTest extends FlatSpec with Matchers {
  behavior of "ProcessMonitor"

  implicit val executionContext = scala.concurrent.ExecutionContext.global
  implicit val log = Utils.getLogbackLogger(this.getClass)
  implicit val timer = new ThreadTimer()

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  it should "terminate process group when max memory reached" in {
    val script = File.createTempFile("test1", ".sh"); script.deleteOnExit()
    IoUtils.writeBytesToFile(script, s"sleep 1\nsh ${script.toString}\nsleep 30".getBytes)

    val process = result(ChildProcess.start("/bin/sh", Seq(script.toString)))
    process.readOutput(onOutput = lines => lines.foreach { case (line, nl) => println(line) })
    new ProcessMonitor(process, RestartConditions(maxMemoryMB = Some(10000), None, false, 100)).start()
    result(process.onTermination())
  }

  it should "terminate process group when max CPU percents reached" in {
    val script = File.createTempFile("test2", ".sh"); script.deleteOnExit()
    IoUtils.writeBytesToFile(script, "for ((;;)) do echo \"test\" >/dev/null; done".getBytes)

    val process = result(ChildProcess.start("/bin/sh", Seq(script.toString)))
    process.readOutput(onOutput = lines => lines.foreach { case (line, nl) => println(line) })
    new ProcessMonitor(process, RestartConditions(None, Some(MaxCpu(50, 2000)), false, 100)).start()
    result(process.onTermination())
  }
}