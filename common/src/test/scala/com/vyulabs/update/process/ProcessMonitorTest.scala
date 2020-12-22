package com.vyulabs.update.process

import com.vyulabs.update.common.common.ThreadTimer
import com.vyulabs.update.common.config.RestartConditions
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

  val script = File.createTempFile("test1", ".sh"); script.deleteOnExit()
  IoUtils.writeBytesToFile(script, s"sleep 1\nsh ${script.toString}\nsleep 30".getBytes)

  it should "terminate process group when max memory reached" in {
    val process = ChildProcess.start("/bin/sh", Seq(script.toString)).get
    process.handleOutput((line, nl) => println(line))
    new ProcessMonitor(process, RestartConditions(maxMemoryMB = Some(10000), false, 100)).start()
    result(process.waitForTermination())
  }
}