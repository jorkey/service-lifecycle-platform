package com.vyulabs.update.distribution.task

import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.common.{IdGenerator, Timer}
import com.vyulabs.update.common.logger.{LogBuffer, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.logger.LogStorekeeper
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

case class TaskLimit(taskType: String, concurrentTasksCount: Option[Int])

case class Task(taskId: TaskId, future: Future[Any], cancel: Option[() => Unit]) {
  val startDate: Date = new Date
}

class TaskManager(logStorekeeper: TaskId => LogStorekeeper)
                 (implicit timer: Timer, executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(getClass)

  private val idGenerator = new IdGenerator()
  private var activeTasks = Map.empty[TaskId, Task]

  def create(taskType: String,
             run: (TaskId, Logger) => (Future[Any], Option[() => Unit])): Task = {
    val taskId = idGenerator.generateId(8)
    log.info(s"Started task ${taskId}")
    val appender = new TraceAppender()
    val logger = Utils.getLogbackLogger(s"Task-${taskId}")
    logger.addAppender(appender)
    val buffer = new LogBuffer(taskType, "TASK", logStorekeeper(taskId), 25, 1000)
    appender.addListener(buffer)
    appender.start()
    val (future, cancel) = run(taskId, logger)
    val task = Task(taskId, future, cancel)
    activeTasks += (taskId -> task)
    future.andThen { case status =>
      log.info(s"Finished task ${taskId} with status ${status}")
      if (status.isSuccess) {
        appender.setTerminationStatus(true, None)
      } else {
        appender.setTerminationStatus(false, Some(status.failed.get.getMessage))
      }
      timer.scheduleOnce(() => appender.stop(),  FiniteDuration(1, TimeUnit.SECONDS))
      activeTasks -= taskId
    }
    task
  }

  def cancel(task: TaskId): Boolean = {
    for (task <- activeTasks.get(task)) {
      for (cancel <- task.cancel) {
        cancel()
        return true
      }
    }
    false
  }

  def getTask(task: TaskId): Option[Task] = {
    activeTasks.get(task)
  }

  def getTasks(): Seq[Task] = {
    activeTasks.values.toSeq.sortBy(_.startDate)
  }
}
