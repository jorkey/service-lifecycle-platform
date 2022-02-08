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

case class TaskInfo(task: TaskId, `type`: TaskType, parameters: Seq[TaskParameter],
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
                 run: (TaskId, Logger) => (Future[Unit], Option[() => Unit]))
                (implicit log: Logger): TaskInfo = {
    synchronized {
      checkAllowToRun()
      val task = taskManager.create(s"task ${taskType} with parameters: ${Misc.seqToCommaSeparatedString(parameters)}", run)
      val info = TaskInfo(task.taskId, taskType, parameters, new Date(), Some(true))
      activeTasks :+= info
      task.future.andThen { case _ => synchronized { activeTasks = activeTasks.filter(_ != info) }}
      collections.Tasks_Info.insert(info.copy(active = None)).failed
        .foreach(ex => log.error("Insert task info error", ex))
      info
    }
  }

  def getTaskTypes(): Future[Seq[TaskType]] ={
    collections.Tasks_Info.distinctField[TaskType]("taskType")
  }

  def getActiveTasks(id: Option[TaskId] = None, taskTypes: Seq[TaskType] = Seq.empty,
                     parameters: Seq[TaskParameter] = Seq.empty): Seq[TaskInfo] = {
    synchronized {
      activeTasks
        .filter(task => id.forall(_ == task.task))
        .filter(task => taskTypes.forall(_ == task.`type`))
        .filter(task => parameters.forall(parameter =>
          task.parameters.exists(_ == parameter)))
        .sortWith((t1, t2) => t1.creationTime.getTime > t2.creationTime.getTime)
    }
  }

  def getTasks(task: Option[TaskId], taskType: Option[String], parameters: Seq[TaskParameter],
               onlyActive: Option[Boolean], limit: Option[Int])
              (implicit log: Logger) : Future[Seq[TaskInfo]] = {
    val activeTasks = getActiveTasks(task, taskType, parameters).take(limit.getOrElse(Int.MaxValue))
    if (onlyActive.getOrElse(false) || limit.exists(_ == activeTasks.size)) {
      Future(activeTasks)
    } else {
      val idArg = task.map { id => Filters.eq("task", id) }
      val taskTypeArg = taskType.map { taskType => Filters.eq("taskType", taskType) }
      val filters = Filters.and((idArg ++ taskTypeArg).asJava)
      val sort = Some(Sorts.descending("_sequence"))
      collections.Tasks_Info.find(filters, sort, limit.map(_ - activeTasks.size))
        .map(_.filter(info => !activeTasks.exists(_.task == info.task))
              .filter(task => parameters.forall(parameter => task.parameters.exists(_ == parameter))))
        .map(activeTasks ++ _)
    }
  }
}
