package com.vyulabs.update.distribution.task

import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.common.{IdGenerator, Timer}
import com.vyulabs.update.common.logger.{LogBuffer, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.logger.LogStorer
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

case class Task(taskId: TaskId, description: String, future: Future[Unit], cancel: Option[() => Unit]) {
  val startDate: Date = new Date
}

class TaskManager(logStorer: TaskId => LogStorer)(implicit timer: Timer, executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(getClass)

  private val idGenerator = new IdGenerator()
  private var activeTasks = Map.empty[TaskId, Task]

  def create(description: String, run: (TaskId, Logger) => (Future[Unit], Option[() => Unit])): Task = {
    val taskId = idGenerator.generateId(8)
    log.info(s"Started task ${taskId} '${description}''")
    val appender = new TraceAppender()
    val logger = Utils.getLogbackLogger(Task.getClass)
    logger.addAppender(appender)
    val buffer = new LogBuffer(description, "TASK", logStorer(taskId), 1, 1000)
    timer.schedulePeriodically(() => buffer.flush(), FiniteDuration(1, TimeUnit.SECONDS))
    appender.addListener(buffer)
    appender.start()
    val (future, cancel) = run(taskId, logger)
    val task = Task(taskId, description, future, cancel)
    activeTasks += (task.taskId -> task)
    future.andThen { case status =>
      log.info(s"Task ${taskId} '${description}' is finished with status ${status}")
      if (status.isSuccess) {
        appender.setTerminationStatus(true, None)
      } else {
        appender.setTerminationStatus(false, Some(status.failed.get.getMessage))
      }
      appender.stop()
      activeTasks -= taskId
    }
    task
  }

  def cancel(taskId: TaskId): Boolean = {
    for (task <- activeTasks.get(taskId)) {
      for (cancel <- task.cancel) {
        cancel()
        return true
      }
    }
    false
  }

  def getTasks(): Seq[Task] = {
    activeTasks.values.toSeq.sortBy(_.startDate)
  }
}
