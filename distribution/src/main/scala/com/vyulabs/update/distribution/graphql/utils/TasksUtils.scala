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
import com.vyulabs.update.distribution.task.{Task, TaskManager}
import org.bson.BsonDocument
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
                    services: Seq[ServiceId], creationTime: Date,
                    terminationTime: Option[Date], terminationStatus: Option[Boolean],
                    expireTime: Date)

trait TasksUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections
  protected val taskManager: TaskManager

  protected val config: DistributionConfig

  private var activeTasks = Seq.empty[TaskInfo]

  def createTask(taskType: TaskType, parameters: Seq[TaskParameter], services: Seq[ServiceId],
                 checkAllowToRun: () => Unit, run: (TaskId, Logger) => (Future[Any], Option[() => Unit]))
                 (implicit log: Logger): Task = {
    synchronized {
      checkAllowToRun()
      val task = taskManager.create(s"task ${taskType} with parameters: ${Misc.seqToCommaSeparatedString(parameters)}",
        run)
      val info = TaskInfo(task.taskId, taskType, parameters, services, new Date(), None, None,
        new Date(System.currentTimeMillis() + config.logs.taskLogExpirationTimeout.toMillis))
      activeTasks :+= info
      task.future.andThen {
        case result =>
          synchronized { activeTasks = activeTasks.filter(_ != info) }
          log.info("task finished")
          collections.Tasks_Info.update(Filters.eq("task", info.task),
            _ => Some(info.copy(terminationTime = Some(new Date()), terminationStatus = Some(result.isSuccess)))).failed
            .foreach(ex => log.error("Update task info error", ex))
      }
      collections.Tasks_Info.insert(info).failed
        .foreach(ex => log.error("Insert task info error", ex))
      task
    }
  }

  def getTaskTypes(): Future[Seq[TaskType]] ={
    collections.Tasks_Info.distinctField[TaskType]("type")
  }

  def getTaskServices(): Future[Seq[ServiceId]] ={
    collections.Tasks_Info.distinctField[ServiceId]("services")
  }

  def getActiveTasks(id: Option[TaskId] = None, taskType: Option[TaskType] = None,
                     parameters: Seq[TaskParameter] = Seq.empty, service: Option[String] = Option.empty): Seq[TaskInfo] = {
    synchronized {
      activeTasks
        .filter(task => id.forall(_ == task.task))
        .filter(task => taskType.forall(_ == task.`type`))
        .filter(task => parameters.forall(parameter =>
          task.parameters.exists(_ == parameter)))
        .filter(task => service.forall(service =>
          task.services.exists(_ == service)))
        .sortWith((t1, t2) => t1.creationTime.getTime > t2.creationTime.getTime)
    }
  }

  def getTasks(task: Option[TaskId], taskType: Option[String], parameters: Seq[TaskParameter], service: Option[String],
               onlyActive: Option[Boolean], limit: Option[Int])(implicit log: Logger) : Future[Seq[TaskInfo]] = {
    val activeTasks = getActiveTasks(task, taskType, parameters, service).take(limit.getOrElse(Int.MaxValue))
    if (onlyActive.getOrElse(false) || limit.exists(_ == activeTasks.size)) {
      Future(activeTasks)
    } else {
      val idArg = task.map { id => Filters.eq("task", id) }
      val taskTypeArg = taskType.map { taskType => Filters.eq("type", taskType) }
      val servicesArg = service.map { service => Filters.eq("services", service) }
      val args = idArg ++ taskTypeArg ++ servicesArg
      val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
      val sort = Some(Sorts.descending("_sequence"))
      collections.Tasks_Info.find(filters, sort, limit.map(_ - activeTasks.size))
        .map(_.filter(info => !activeTasks.exists(_.task == info.task))
              .filter(task => parameters.forall(parameter => task.parameters.exists(_ == parameter))))
        .map(activeTasks ++ _)
    }
  }
}
