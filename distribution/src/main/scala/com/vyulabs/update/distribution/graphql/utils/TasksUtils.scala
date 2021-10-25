package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.common.Misc
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.distribution.mongo._
import com.vyulabs.update.distribution.task.TaskManager
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol

import java.util.Date
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

case class TaskParameter(name: String, value: String) {
  override def toString: String = s"${name}=${value}"
}

object TaskParameter extends DefaultJsonProtocol {
  implicit val taskParameterJson = jsonFormat2(TaskParameter.apply)
}

case class TaskInfo(id: TaskId, taskType: TaskType, parameters: Seq[TaskParameter],
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

  def createTask(taskType: TaskType, parameters: Seq[TaskParameter],
                 checkAllowToRun: () => Unit,
                 run: (TaskId, Logger) => (Future[Unit], Option[() => Unit])): TaskInfo = {
    synchronized {
      checkAllowToRun()
      val task = taskManager.create(s"Task ${taskType} with parameters: ${Misc.seqToCommaSeparatedString(parameters)}", run)
      val info = TaskInfo(task.taskId, taskType, parameters, new Date(), Some(true))
      activeTasks :+= info
      collections.Tasks_Info.insert(info.copy(active = None))
      task.future.andThen { case _ => synchronized { activeTasks = activeTasks.filter(_ != info) }}
      info
    }
  }

  def getTaskTypes(): Future[Seq[TaskType]] ={
    collections.Tasks_Info.distinctField[TaskType]("taskType")
  }

  def getActiveTasks(taskType: Option[TaskType],
                     parameters: Seq[TaskParameter] = Seq.empty): Seq[TaskInfo] = {
    synchronized {
      activeTasks
        .filter(task => taskType.forall(_ == task.taskType))
        .filter(task => parameters.forall(parameter =>
          task.parameters.exists(_ == parameter)))
        .sortWith((t1, t2) => t1.creationTime.getTime > t2.creationTime.getTime)
    }
  }

  def getTasks(taskType: Option[String], parameters: Seq[TaskParameter],
               onlyActive: Option[Boolean], limit: Option[Int])
              (implicit log: Logger) : Future[Seq[TaskInfo]] = {
    val activeTasks = getActiveTasks(taskType, parameters)
    if (onlyActive.getOrElse(false)) {
      Future(activeTasks.take(limit.getOrElse(activeTasks.size)))
    } else {
      val taskTypeArg = taskType.map { taskType => Filters.eq("taskType", taskType) }
      val filters = Filters.and(taskTypeArg.toSeq.asJava)
      val sort = Some(Sorts.descending("_sequence"))
      collections.Tasks_Info.find(filters, sort, limit).map(
        _.filter(task => parameters.forall(parameter =>
          task.parameters.exists(_ == parameter)))
        .map(task => task.copy(active = activeTasks.find(_.id == task.id).map(_ => true))))
    }
  }
}
