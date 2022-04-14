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

case class SequencedTaskInfo(sequence: BigInt, task: TaskId, `type`: TaskType, parameters: Seq[TaskParameter],
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

  def createTask(taskType: TaskType, parameters: Seq[TaskParameter], services: Seq[ServiceId],
                 run: (TaskId, Logger) => (Future[Any], Option[() => Unit]))
                 (implicit log: Logger): Future[Task] = {
    synchronized {
      val task = taskManager.create(s"task ${taskType} with parameters: ${Misc.seqToCommaSeparatedString(parameters)}",
        run)
      val info = TaskInfo(task.taskId, taskType, parameters, services, new Date(), None, None,
        new Date(System.currentTimeMillis() + config.logs.taskLogExpirationTimeout.toMillis))
      task.future.andThen {
        case result =>
          collections.Tasks_Info.update(Filters.eq("task", info.task),
            _ => Some(info.copy(terminationTime = Some(new Date()), terminationStatus = Some(result.isSuccess))))
            .failed.foreach(ex => log.error("Update task info error", ex))
      }
      collections.Tasks_Info.insert(info).map(_ => task)
    }
  }

  def getTaskTypes(): Future[Seq[TaskType]] ={
    collections.Tasks_Info.distinctField[TaskType]("type")
  }

  def getTaskServices(): Future[Seq[ServiceId]] ={
    collections.Tasks_Info.distinctField[ServiceId]("services")
  }

  def getTasks(task: Option[TaskId]=None, taskType: Option[String]=None, parameters: Seq[TaskParameter]=Seq.empty,
               service: Option[String]=None, onlyActive: Option[Boolean]=None,
               fromTime: Option[Date]=None, toTime: Option[Date]=None, from: Option[BigInt], limit: Option[Int]=None)
              (implicit log: Logger) : Future[Seq[SequencedTaskInfo]] = {
    val idArg = task.map { id => Filters.eq("task", id) }
    val taskTypeArg = taskType.map { taskType => Filters.eq("type", taskType) }
    val servicesArg = service.map { service => Filters.eq("services", service) }
    val onlyActiveArg = onlyActive.filter(onlyActive => onlyActive)
      .map { _ => Filters.eq("terminationStatus", null) }
    val fromTimeArg = fromTime.map(time => Filters.gte("creationTime", time))
    val toTimeArg = toTime.map(time => Filters.lte("creationTime", time))
    val fromArg = from.map(sequence => Filters.lte("_sequence", sequence.toLong))
    val args = idArg ++ taskTypeArg ++ servicesArg ++ onlyActiveArg ++ fromTimeArg ++ toTimeArg ++ fromArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Some(Sorts.descending("_sequence"))
    collections.Tasks_Info.findSequenced(filters, sort, limit)
      .map(_.filter(task => parameters.forall(parameter => task.document.parameters.exists(_ == parameter))))
            .map(_.map(task => SequencedTaskInfo(task.sequence, task.document.task, task.document.`type`,
              task.document.parameters, task.document.services, task.document.creationTime,
              task.document.terminationTime, task.document.terminationStatus, task.document.expireTime)))
  }
}
