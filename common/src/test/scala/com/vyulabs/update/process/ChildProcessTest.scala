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
  IoUtils.writeBytesToFile(script, "echo \"message1\"; echo \"message2\"; echo -n \"message3 \"\nsleep 5\necho \"message4\"".getBytes)

  val errorScript = File.createTempFile("test2", ".sh"); errorScript.deleteOnExit()
  IoUtils.writeBytesToFile(errorScript, "echo \"message\"\nsleep 5\necho \"error message\" >&2".getBytes)

  it should "run process and read output" in {
    val process = result(ChildProcess.start("/bin/bash", Seq(script.toString)))
    process.readOutput(onOutput = lines => lines.foreach { case (line, nl) => println(line) })
    result(process.onTermination())
  }

  it should "run process and read error output" in {
    val process = result(ChildProcess.start("/bin/bash", Seq(errorScript.toString)))
    process.readOutput(onOutput = lines => lines.foreach { case (line, nl) => println(line) })
    result(process.onTermination())
  }

  it should "terminate process" in {
    val process = result(ChildProcess.start("/bin/bash", Seq(script.toString)))
    process.readOutput(lines => lines.foreach { case (line, nl) =>
      assertResult(false)(result(process.terminate()))
    })
    result(process.onTermination())
  }
}