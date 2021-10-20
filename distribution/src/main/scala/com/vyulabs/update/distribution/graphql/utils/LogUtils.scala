package com.vyulabs.update.distribution.graphql.utils

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.mongo._
import com.vyulabs.update.distribution.task.TaskInfo
import org.bson.BsonDocument
import org.slf4j.Logger
import sangria.schema.Action

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait LogUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def addLogs(service: ServiceId, instance: InstanceId, directory: ServiceDirectory, process: ProcessId,
              task: Option[TaskId], logs: Seq[LogLine])(implicit log: Logger): Future[Unit] = {
    val documents = logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => { seq :+
      ServiceLogLine(service, instance, directory, process, task, line) })
    collections.Log_Lines.insert(documents).map(_ => ())
  }

  def getLogServices()(implicit log: Logger): Future[Seq[ServiceId]] = {
    collections.Log_Lines.distinctField[String]("service")
  }

  def getLogInstances(service: ServiceId)(implicit log: Logger): Future[Seq[InstanceId]] = {
    val serviceArg = Filters.eq("service", service)
    val filters = Filters.and(Seq(serviceArg).asJava)
    collections.Log_Lines.distinctField[String]("instance", filters)
  }

  def getLogDirectories(service: ServiceId, instance: InstanceId)(implicit log: Logger): Future[Seq[ServiceDirectory]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val filters = Filters.and(Seq(serviceArg, instanceArg).asJava)
    collections.Log_Lines.distinctField[String]("directory", filters)
  }

  def getLogProcesses(service: ServiceId, instance: InstanceId, directory: ServiceDirectory)
                     (implicit log: Logger): Future[Seq[ProcessId]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val directoryArg = Filters.eq("directory", directory)
    val filters = Filters.and(Seq(serviceArg, instanceArg, directoryArg).asJava)
    collections.Log_Lines.distinctField[String]("process", filters)
  }

  def getLogLevels(service: Option[ServiceId], instance: Option[InstanceId],
                   directory: Option[ServiceDirectory], process: Option[ProcessId], task: Option[TaskId])
             (implicit log: Logger): Future[Seq[String]] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg
    val filters = Filters.and(args.asJava)
    collections.Log_Lines.distinctField[String]("payload.level", filters)
  }

  def getLogsStartTime(service: Option[ServiceId], instance: Option[InstanceId],
                       directory: Option[ServiceDirectory], process: Option[ProcessId], task: Option[TaskId])
                      (implicit log: Logger): Future[Option[Date]] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg
    val filters = Filters.and(args.asJava)
    val sort = Sorts.ascending("payload.time")
    collections.Log_Lines.find(filters, Some(sort), Some(1)).map(_.headOption.map(_.payload.time))
  }

  def getLogsEndTime(service: Option[ServiceId], instance: Option[InstanceId],
                     directory: Option[ServiceDirectory], process: Option[ProcessId], task: Option[TaskId])
                    (implicit log: Logger): Future[Option[Date]] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg
    val filters = Filters.and(args.asJava)
    val sort = Sorts.descending("payload.time")
    collections.Log_Lines.find(filters, Some(sort), Some(1)).map(_.headOption
      .find(_.payload.terminationStatus.isDefined).map(_.payload.time))
  }

  def getLogs(service: Option[ServiceId], instance: Option[InstanceId],
              directory: Option[ServiceDirectory], process: Option[ProcessId], task: Option[TaskId],
              from: Option[BigInt], to: Option[BigInt],
              fromTime: Option[Date], toTime: Option[Date],
              levels: Option[Seq[String]], find: Option[String], limit: Option[Int])
             (implicit log: Logger): Future[Seq[SequencedServiceLogLine]] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val processArg = process.map(Filters.eq("process", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val taskArg = task.map(Filters.eq("task", _))
    val fromArg = from.map(sequence => Filters.gte("_sequence", sequence.toLong))
    val toArg = to.map(sequence => Filters.lte("_sequence", sequence.toLong))
    val fromTimeArg = fromTime.map(time => Filters.gte("payload.time", time))
    val toTimeArg = toTime.map(time => Filters.lte("payload.time", time))
    val levelsArg = levels.map(levels =>
      Filters.or(levels.map(level => Filters.eq("payload.level", level)).asJava))
    val findArg = find.map(text => Filters.text(text))
    val args = serviceArg ++ instanceArg ++ processArg ++ directoryArg ++ taskArg ++
      fromArg ++ toArg ++ fromTimeArg ++ toTimeArg ++ levelsArg ++ findArg
    val filters = Filters.and(args.asJava)
    val sort = if (to.isEmpty || !from.isEmpty) Sorts.ascending("_sequence") else Sorts.descending("_sequence")
    collections.Log_Lines.findSequenced(filters, Some(sort), limit)
      .map(_.sortBy(_.sequence))
      .map(_.map(line => SequencedServiceLogLine(line.sequence,
        line.document.instance, line.document.directory, line.document.process, line.document.payload)))
  }

  def subscribeLogs(service: Option[ServiceId],
                    instance: Option[InstanceId], directory: Option[ServiceDirectory], process: Option[ProcessId],
                    task: Option[TaskId], from: Option[Long], prefetch: Option[Int], levels: Option[Seq[String]])
                   (implicit log: Logger): Source[Action[Nothing, Seq[SequencedServiceLogLine]], NotUsed] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val processArg = process.map(Filters.eq("process", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ processArg ++ directoryArg ++ taskArg
    val filters = Filters.and(args.asJava)
    val source = collections.Log_Lines.subscribe(filters, from, prefetch)
      .filter(log => service.isEmpty || service.contains(log.document.service))
      .filter(log => instance.isEmpty || instance.contains(log.document.instance))
      .filter(log => process.isEmpty || process.contains(log.document.process))
      .filter(log => directory.isEmpty || directory.contains(log.document.directory))
      .filter(log => task.isEmpty || task == log.document.task)
      .filter(log => levels.isEmpty || levels.get.contains(log.document.payload.level))
      .takeWhile(!_.document.payload.terminationStatus.isDefined, true)
      .groupedWeightedWithin(25, FiniteDuration.apply(100, TimeUnit.MILLISECONDS))(_ => 1)
      .map(lines => Action(lines.map(line => SequencedServiceLogLine(line.sequence,
        line.document.instance, line.document.directory, line.document.process, line.document.payload))))
      .buffer(100, OverflowStrategy.fail)
    source.mapMaterializedValue(_ => NotUsed)
  }

  def testSubscription()(implicit log: Logger): Source[Action[Nothing, String], NotUsed] = {
    Source.tick(FiniteDuration(1, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.SECONDS), Action("line"))
      .mapMaterializedValue(_ => NotUsed).take(5)
  }
}
