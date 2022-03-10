package com.vyulabs.update.distribution.logger

import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.TestEnvironment

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

class TaskLoggingTest extends TestEnvironment with ScalatestRouteTest {
  behavior of "Task logs writing"

  it should "write task logs" in {
    val task = taskManager.create("Test task", (taskId, log) => {(
      Future {
        log.info("log line 1")
        log.warn("log line 2")
        log.error("log line 3")
        log.warn("log line 4")
        log.info("log line 5") },
      None)
    })
    Await.result(task.future, FiniteDuration.apply(10, TimeUnit.SECONDS))
    Thread.sleep(1000)
    val linesFuture = collections.Log_Lines.find(Filters.eq("task", task.taskId))
    val lines = Await.result(linesFuture, FiniteDuration.apply(10, TimeUnit.SECONDS))
      .map(_.message).drop(1).dropRight(1)
    assertResult(Seq("log line 1", "log line 2", "log line 3", "log line 4", "log line 5"))(lines)
  }

  it should "write concurrently tasks logs" in {
    val task1 = taskManager.create("Test task1", (taskId, log) => {(
      Future {
        Thread.sleep(100)
        for (i <- 1 to 1000) {
          log.info(s"task1 - log line ${i}")
        }
      },
      None)
    })
    val task2 = taskManager.create("Test task2", (taskId, log) => {(
      Future {
        for (i <- 1 to 1000) {
          log.info(s"task2 - log line ${i}")
        }
      },
      None)
    })
    Await.result(task1.future, FiniteDuration.apply(10, TimeUnit.SECONDS))
    Await.result(task2.future, FiniteDuration.apply(10, TimeUnit.SECONDS))
    Thread.sleep(1000)

    val linesFuture1 = collections.Log_Lines.find(Filters.eq("task", task1.taskId))
    val lines1 = Await.result(linesFuture1, FiniteDuration.apply(10, TimeUnit.SECONDS))
      .map(_.message).drop(1).dropRight(1)
    assertResult((1 to 1000).map(i => s"task1 - log line ${i}"))(lines1)

    val linesFuture2 = collections.Log_Lines.find(Filters.eq("task", task2.taskId))
    val lines2 = Await.result(linesFuture2, FiniteDuration.apply(10, TimeUnit.SECONDS))
      .map(_.message).drop(1).dropRight(1)
    assertResult((1 to 1000).map(i => s"task2 - log line ${i}"))(lines2)
  }
}
