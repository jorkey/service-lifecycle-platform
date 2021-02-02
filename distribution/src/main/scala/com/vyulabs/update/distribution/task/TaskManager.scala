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

case class Task(taskId: TaskId, description: String, future: Future[_], cancel: Option[() => Unit]) {
  val startDate: Date = new Date
}

class TaskManager(logStorer: TaskId => LogStorer)(implicit timer: Timer, executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(getClass)

  private val idGenerator = new IdGenerator()
  private var activeTasks = Map.empty[TaskId, Task]

  def create[T](description: String, run: (TaskId, Logger) => (Future[T], Option[() => Unit])): Task = {
    val taskId = idGenerator.generateId(8)
    val (future, cancel) = {
      val appender = new TraceAppender(); appender.start()
      val logger = Utils.getLogbackLogger(Task.getClass)
      logger.addAppender(appender)
      val buffer = new LogBuffer(logStorer(taskId), 10, 1000)
      timer.schedulePeriodically(() => buffer.flush(), FiniteDuration(1, TimeUnit.SECONDS))
      appender.addListener(buffer)
      run(taskId, logger)
    }
    val task = Task(taskId, description, future, cancel)
    activeTasks += (task.taskId -> task)
    log.info(s"Create new task ${task.taskId}")
    task.future.andThen {
      case result =>
        log.info(s"Task ${task.taskId} is terminated with result ${result}")
        activeTasks -= task.taskId
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
