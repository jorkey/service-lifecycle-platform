package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.mongo._
import com.vyulabs.update.distribution.task.{TaskManager}
import org.slf4j.Logger

import java.util.Date
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

case class TaskAttribute(name: String, value: String)

case class TaskInfo(taskId: TaskId, taskType: String, attributes: Seq[TaskAttribute],
                    creationTime: Date, active: Option[Boolean] = None)

trait TasksUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val taskManager: TaskManager

  protected val config: DistributionConfig

  private var activeTasks = Seq.empty[TaskInfo]

  def createTask(taskType: String, attributes: Seq[TaskAttribute],
                 checkAllowToRun: () => Unit,
                 run: (TaskId, Logger) => (Future[Unit], Option[() => Unit])): TaskInfo = {
    synchronized {
      checkAllowToRun()
      val task = taskManager.create(run)
      val info = TaskInfo(task.taskId, taskType, attributes, new Date(), Some(true))
      activeTasks :+= info
      collections.Tasks_Info.insert(info.copy(active = None))
      task.future.andThen { case _ => synchronized { activeTasks = activeTasks.filter(_ != info) }}
      info
    }
  }

  def getActiveTask(taskType: String, attribute: Option[TaskAttribute] = None)
      : Option[TaskInfo] = {
    synchronized {
      activeTasks
        .find(_.taskType == taskType)
        .find(task => { attribute match {
          case Some(attr) =>
            task.attributes.exists(_ == attr)
          case None =>
            true
        }})
    }
  }

  def getTasks(taskType: Option[String], limit: Option[Int])(implicit log: Logger)
      : Future[Seq[TaskInfo]] = {
    val taskTypeArg = taskType.map { taskType => Filters.eq("taskType", taskType) }
    val filters = Filters.and(taskTypeArg.toSeq.asJava)
    val sort = Some(Sorts.descending("_sequence"))
    collections.Tasks_Info.find(filters, sort, limit)
  }
}
