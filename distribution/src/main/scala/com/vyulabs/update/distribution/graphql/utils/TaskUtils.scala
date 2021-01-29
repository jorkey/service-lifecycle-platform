package com.vyulabs.update.distribution.graphql.utils

import com.vyulabs.update.common.common.Common.TaskId
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger

import scala.concurrent.Future

trait TaskUtils {
  protected val taskManager: TaskManager

  def createTask[T](description: String, run: (TaskId, Logger) => Future[T], cancel: Option[() => Unit])(implicit log: Logger): TaskId = {
    taskManager.create(description, run, cancel).taskId
  }

  def cancelTask(taskId: TaskId)(implicit log: Logger): Boolean = {
    taskManager.cancel(taskId)
  }
}
