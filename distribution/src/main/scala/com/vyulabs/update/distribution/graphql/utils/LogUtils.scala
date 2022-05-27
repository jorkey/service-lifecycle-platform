package com.vyulabs.update.distribution.graphql.utils

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.mongo._
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
    val expirationTimeout = if (task.isDefined) config.logs.taskLogExpirationTimeout else config.logs.serviceLogExpirationTimeout
    val expireTime = new Date(System.currentTimeMillis() + expirationTimeout.toMillis)
    val documents = logs.map(line =>
      ServiceLogLine(
        service = service,
        instance = instance,
        directory = directory,
        process = process,
        task = task,
        time = line.time,
        level = line.level,
        unit = line.unit,
        message = line.message,
        terminationStatus = line.terminationStatus,
        expireTime = expireTime
      ))
    collections.Log_Lines.insert(documents).map(_ => ())
  }

  def getLogServices()(implicit log: Logger): Future[Seq[ServiceId]] = {
    collections.Log_Lines.distinctField[String]("service")
  }

  def getLogInstances(service: ServiceId)(implicit log: Logger): Future[Seq[InstanceId]] = {
    val serviceArg = Filters.eq("service", service)
    val filters = serviceArg
    collections.Log_Lines.distinctField[String]("instance", filters)
  }

  def getLogDirectories(service: ServiceId, instance: InstanceId)(implicit log: Logger): Future[Seq[ServiceDirectory]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val args = Seq(serviceArg, instanceArg)
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Log_Lines.distinctField[String]("directory", filters)
  }

  def getLogProcesses(service: ServiceId, instance: InstanceId, directory: ServiceDirectory)
                     (implicit log: Logger): Future[Seq[ProcessId]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val directoryArg = Filters.eq("directory", directory)
    val args = Seq(serviceArg, instanceArg, directoryArg)
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
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
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.Log_Lines.distinctField[String]("level", filters)
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
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Sorts.ascending("_sequence")
    collections.Log_Lines.find(filters, Some(sort), Some(1)).map(_.headOption.map(_.time))
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
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Sorts.descending("_sequence")
    collections.Log_Lines.find(filters, Some(sort), Some(1)).map(_.headOption.map(_.time))
  }

  def getLogs(service: Option[ServiceId] = None, instance: Option[InstanceId] = None,
              directory: Option[ServiceDirectory] = None, process: Option[ProcessId] = None, task: Option[TaskId] = None,
              levels: Option[Seq[String]] = None, find: Option[String] = None,
              fromTime: Option[Date] = None, toTime: Option[Date] = None, from: Option[Long] = None, to: Option[Long] = None,
              limit: Option[Int] = None)
             (implicit log: Logger): Future[Seq[SequencedServiceLogLine]] = {
    getLogsToStream(service, instance, directory, process, task, levels, find, fromTime, toTime, from, to, limit)
      .runWith(Sink.fold[Seq[SequencedServiceLogLine], SequencedServiceLogLine](Seq.empty[SequencedServiceLogLine])
        ((seq, obj) => {seq :+ obj})).map(_.sortBy(_.sequence))
  }

  def getLogsToStream(service: Option[ServiceId] = None, instance: Option[InstanceId] = None,
              directory: Option[ServiceDirectory] = None, process: Option[ProcessId] = None, task: Option[TaskId] = None,
              levels: Option[Seq[String]] = None, find: Option[String] = None,
              fromTime: Option[Date] = None, toTime: Option[Date] = None, from: Option[Long] = None, to: Option[Long] = None,
              limit: Option[Int] = None)
             (implicit log: Logger): Source[SequencedServiceLogLine, NotUsed] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val levelsArg = levels.map(levels =>
      Filters.or(levels.map(level => Filters.eq("level", level)).asJava))
    val findArg = find.map(text => Filters.text(text))
    val fromTimeArg = fromTime.map(time => Filters.gte("time", time))
    val toTimeArg = toTime.map(time => Filters.lte("time", time))
    val fromArg = from.map(sequence => Filters.gte("_sequence", sequence.toLong))
    val toArg = to.map(sequence => Filters.lte("_sequence", sequence.toLong))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg ++
      levelsArg ++ findArg ++ fromTimeArg ++ toTimeArg ++ fromArg ++ toArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = if (to.isEmpty || !from.isEmpty) Sorts.ascending("_sequence") else Sorts.descending("_sequence")
    collections.Log_Lines.findSequencedToStream(filters, Some(sort), limit)
      .map(line => SequencedServiceLogLine(
        sequence = line.sequence,
        service = line.document.service,
        instance = line.document.instance,
        directory = line.document.directory,
        process = line.document.process,
        task = line.document.task,
        time = line.document.time,
        level = line.document.level,
        unit = line.document.unit,
        message = line.document.message,
        terminationStatus = line.document.terminationStatus))
  }

  def subscribeLogs(service: Option[ServiceId],
                    instance: Option[InstanceId], directory: Option[ServiceDirectory], process: Option[ProcessId],
                    task: Option[TaskId], levels: Option[Seq[String]], unit: Option[String] = None, find: Option[String] = None,
                    from: Option[Long])
                   (implicit log: Logger): Source[Action[Nothing, Seq[SequencedServiceLogLine]], NotUsed] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg
    val filters = Filters.and(args.asJava)
    val source = collections.Log_Lines.subscribe(filters, from)
      .filter(log => service.isEmpty || service.contains(log.document.service))
      .filter(log => instance.isEmpty || instance.contains(log.document.instance))
      .filter(log => directory.isEmpty || directory.contains(log.document.directory))
      .filter(log => process.isEmpty || process.contains(log.document.process))
      .filter(log => task.isEmpty || task == log.document.task)
      .takeWhile(log => task.isEmpty || !log.document.terminationStatus.isDefined, true)
      .filter(log => levels.isEmpty || levels.get.contains(log.document.level))
      .filter(log => unit.isEmpty || unit.contains(log.document.unit))
      .filter(log => find.isEmpty || log.document.message.toLowerCase().contains(find.get.toLowerCase))
      .groupedWeightedWithin(100, FiniteDuration.apply(500, TimeUnit.MILLISECONDS))(_ => 1)
      .map(lines => Action(lines.map(line => SequencedServiceLogLine(
        sequence = line.sequence,
        instance = line.document.instance,
        service = line.document.service,
        directory = line.document.directory,
        process = line.document.process,
        task = line.document.task,
        time = line.document.time,
        level = line.document.level,
        unit = line.document.unit,
        message = line.document.message,
        terminationStatus = line.document.terminationStatus))))
      .buffer(10000, OverflowStrategy.fail)
    source.mapMaterializedValue(_ => NotUsed)
  }

  def testSubscription()(implicit log: Logger): Source[Action[Nothing, String], NotUsed] = {
    val spaces = new String((0 to 100).map(_ => ' ').toArray)
    Source.fromIterator(() => (0 to 1000).map(i => Action(spaces + i.toString)).iterator)
          .mapMaterializedValue(_ => NotUsed)
  }
}
