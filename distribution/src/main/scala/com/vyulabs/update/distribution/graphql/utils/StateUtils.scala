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
import org.bson.BsonDocument
import org.slf4j.Logger
import sangria.schema.Action

import java.util.Date
import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

trait StateUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def setTestedVersions(distribution: DistributionId, servicesProfile: ServicesProfileId,
                        desiredVersions: Seq[DeveloperDesiredVersion])(implicit log: Logger): Future[Unit] = {
    for {
      testedVersions <- getTestedVersions(servicesProfile)
      result <- {
        val testRecord = TestSignature(distribution, new Date())
        val testSignatures = testedVersions match {
          case Some(testedVersions) if testedVersions.versions.equals(desiredVersions) =>
            testedVersions.signatures :+ testRecord
          case _ =>
            Seq(testRecord)
        }
        val newTestedVersions = TestedDesiredVersions(servicesProfile, desiredVersions, testSignatures)
        val profileArg = Filters.eq("servicesProfile", servicesProfile)
        collections.Developer_TestedVersions.update(profileArg, _ =>
          Some(newTestedVersions)).map(_ => ())
      }
    } yield result
  }

  def getTestedVersions(servicesProfile: ServicesProfileId)(implicit log: Logger): Future[Option[TestedDesiredVersions]] = {
    val profileArg = Filters.eq("servicesProfile", servicesProfile)
    collections.Developer_TestedVersions.find(profileArg).map(_.headOption)
  }

  def setSelfServiceStates(states: Seq[DirectoryServiceState])(implicit log: Logger): Future[Unit] = {
    setServiceStates(config.distribution, states.map(state => InstanceServiceState(config.instance, state.service, state.directory, state.state)))
  }

  def setServiceStates(distribution: DistributionId, instanceStates: Seq[InstanceServiceState])(implicit log: Logger): Future[Unit] = {
    val documents = instanceStates.foldLeft(Seq.empty[DistributionServiceState])((seq, state) => seq :+ DistributionServiceState(distribution, state))
    Future.sequence(documents.map(doc => {
      val filters = Filters.and(
        Filters.eq("distribution", distribution),
        Filters.eq("payload.service", doc.payload.service),
        Filters.eq("payload.instance", doc.payload.instance),
        Filters.eq("payload.directory", doc.payload.directory))
      collections.State_ServiceStates.update(filters, _ => Some(doc))
    })).map(_ => ())
  }

  def getServicesState(distribution: Option[DistributionId], service: Option[ServiceId],
                       instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[DistributionServiceState]] = {
    val distributionArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val serviceArg = service.map { service => Filters.eq("payload.service", service) }
    val instanceArg = instance.map { instance => Filters.eq("payload.instance", instance) }
    val directoryArg = directory.map { directory => Filters.eq("payload.directory", directory) }
    val args = distributionArg ++ serviceArg ++ instanceArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ServiceStates.find(filters)
  }

  def getInstanceServiceStates(distribution: Option[DistributionId], service: Option[ServiceId],
                               instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[InstanceServiceState]] = {
    getServicesState(distribution, service, instance, directory).map(_.map(_.payload))
  }

  def addLogs(service: ServiceId, instance: InstanceId, directory: ServiceDirectory, process: ProcessId,
              task: Option[TaskId], logs: Seq[LogLine])(implicit log: Logger): Future[Unit] = {
    val documents = logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => { seq :+
      ServiceLogLine(service, instance, directory, process, task, line) })
    collections.State_ServiceLogs.insert(documents).map(_ => ())
  }

  def getLogServices()(implicit log: Logger): Future[Seq[ServiceId]] = {
    collections.State_ServiceLogs.distinctField[String]("service")
  }

  def getLogInstances(service: ServiceId)(implicit log: Logger): Future[Seq[InstanceId]] = {
    val serviceArg = Filters.eq("service", service)
    val filters = Filters.and(Seq(serviceArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("instance", filters)
  }

  def getLogDirectories(service: ServiceId, instance: InstanceId)(implicit log: Logger): Future[Seq[ServiceDirectory]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val filters = Filters.and(Seq(serviceArg, instanceArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("directory", filters)
  }

  def getLogProcesses(service: ServiceId, instance: InstanceId, directory: ServiceDirectory)
                     (implicit log: Logger): Future[Seq[ProcessId]] = {
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val directoryArg = Filters.eq("directory", directory)
    val filters = Filters.and(Seq(serviceArg, instanceArg, directoryArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("process", filters)
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
    collections.State_ServiceLogs.distinctField[String]("payload.level", filters)
  }

  def getLogTimes(service: Option[ServiceId], instance: Option[InstanceId],
                  directory: Option[ServiceDirectory], process: Option[ProcessId], task: Option[TaskId])
                  (implicit log: Logger): Future[Seq[Date]] = {
    val serviceArg = service.map(Filters.eq("service", _))
    val instanceArg = instance.map(Filters.eq("instance", _))
    val directoryArg = directory.map(Filters.eq("directory", _))
    val processArg = process.map(Filters.eq("process", _))
    val taskArg = task.map(Filters.eq("task", _))
    val args = serviceArg ++ instanceArg ++ directoryArg ++ processArg ++ taskArg
    val filters = Filters.and(args.asJava)
    collections.State_ServiceLogs.distinctField[Date]("payload.time", filters)
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
    collections.State_ServiceLogs.findSequenced(filters, Some(sort), limit)
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
    val source = collections.State_ServiceLogs.subscribe(filters, from, prefetch)
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

  def addServiceFaultReportInfo(distribution: DistributionId, report: ServiceFaultReport)(implicit log: Logger): Future[Unit] = {
    for {
      result <- collections.State_FaultReportsInfo.insert(DistributionFaultReport(distribution, report)).map(_ => ())
      _ <- clearOldReports()
    } yield result
  }

  def getDistributionFaultReportsInfo(distribution: Option[DistributionId], service: Option[ServiceId],
                                      last: Option[Int])(implicit log: Logger)
      : Future[Seq[DistributionFaultReport]] = {
    val clientArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val serviceArg = service.map { service => Filters.eq("payload.info.service", service) }
    val args = clientArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
    val sort = last.map { last => Sorts.descending("_sequence") }
    collections.State_FaultReportsInfo.find(filters, sort, last)
  }

  private def clearOldReports()(implicit log: Logger): Future[Unit] = {
    for {
      reports <- collections.State_FaultReportsInfo.findSequenced()
      result <- {
        val remainReports = reports
          .sortBy(_.document.payload.info.time)
          .filter(_.document.payload.info.time.getTime +
            config.faultReports.expirationTimeout.toMillis >= System.currentTimeMillis())
          .takeRight(config.faultReports.maxReportsCount)
        deleteReports(collections.State_FaultReportsInfo, reports.toSet -- remainReports.toSet)
      }
    } yield result
  }

  private def deleteReports(collection: SequencedCollection[DistributionFaultReport], reports: Set[Sequenced[DistributionFaultReport]])
                           (implicit log: Logger): Future[Unit] = {
    Future.sequence(reports.map { report =>
      log.debug(s"Delete fault report ${report.sequence}")
      val faultFile = directory.getFaultReportFile(report.document.payload.faultId)
      faultFile.delete()
      collection.delete(Filters.and(Filters.eq("_sequence", report.sequence)))
    }).map(_ => Unit)
  }
}
