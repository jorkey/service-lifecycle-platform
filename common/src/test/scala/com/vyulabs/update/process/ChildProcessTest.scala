package com.vyulabs.update.process

import com.vyulabs.update.common.process.ChildProcess
import com.vyulabs.update.common.utils.{IoUtils, Utils}
import org.scalatest.{FlatSpec, Matchers}

import java.io.File
import java.util.concurrent.TimeUnit
import scala.concurrent.{Await, Awaitable}
import scala.concurrent.duration.FiniteDuration

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 22.12.20.
  * Copyright FanDate, Inc.
  */
class ChildProcessTest extends FlatSpec with Matchers {
  behavior of "ChildProcess"

  implicit val executionContext = scala.concurrent.ExecutionContext.global
  implicit val log = Utils.getLogbackLogger(this.getClass)

  def result[T](awaitable: Awaitable[T]) = Await.result(awaitable, FiniteDuration(15, TimeUnit.SECONDS))

  val script = File.createTempFile("test1", ".sh"); script.deleteOnExit()
  IoUtils.writeBytesToFile(script, "echo \"message1\"\nsleep 5\necho \"message2\"".getBytes)

  it should "run process and read output" in {
    val process = ChildProcess.start("/bin/sh", Seq(script.toString)).get
    process.handleOutput((line, nl) => {
      println(line)
    })
    result(process.waitForTermination())
  }

  it should "terminate process" in {
    val process = ChildProcess.start("/bin/sh", Seq(script.toString)).get
    process.handleOutput((line, nl) => {
      println(line)
      assertResult(false)(result(process.terminate()))
    })
    result(process.waitForTermination())
  }
}