package com.vyulabs.update.distribution.task

import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.common.common.{IdGenerator, Timer}
import com.vyulabs.update.common.logger.{LogBuffer, TraceAppender}
import com.vyulabs.update.common.utils.Utils
import com.vyulabs.update.distribution.logger.LogStorekeeper
import org.slf4j.{Logger, LoggerFactory}

import java.util.Date
import scala.concurrent.{ExecutionContext, Future}

case class TaskAttribute(name: String, value: String)

case class TaskInfo(task: TaskId, taskType: String, attributes: Seq[TaskAttribute], creationTime: Date)

case class Task(info: TaskInfo, future: Future[Unit], cancel: Option[() => Unit]) {
  val startDate: Date = new Date
}

class TaskManager(logStorekeeper: TaskId => LogStorekeeper)(implicit timer: Timer, executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(getClass)

  private val idGenerator = new IdGenerator()
  private var activeTasks = Map.empty[TaskId, Task]

  def create(taskType: String, attributes: Seq[TaskAttribute], run: (TaskId, Logger) => (Future[Unit], Option[() => Unit])): Task = {
    val taskId = idGenerator.generateId(8)
    val description = s"Started task ${taskId}, type ${taskType}, attributes '${attributes}'"
    log.info(description)
    val appender = new TraceAppender()
    val logger = Utils.getLogbackLogger(Task.getClass)
    logger.addAppender(appender)
    val buffer = new LogBuffer(description, "TASK", logStorekeeper(taskId), 1, 1000)
    appender.addListener(buffer)
    appender.start()
    val (future, cancel) = run(taskId, logger)
    val task = Task(TaskInfo(taskId, taskType, attributes, new Date()), future, cancel)
    activeTasks += (task.info.task -> task)
    future.andThen { case status =>
      log.info(s"Task ${task} is finished with status ${status}")
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

  def cancel(task: TaskId): Boolean = {
    for (task <- activeTasks.get(task)) {
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
