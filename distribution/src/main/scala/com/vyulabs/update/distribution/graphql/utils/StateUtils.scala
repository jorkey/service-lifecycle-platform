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
        Filters.eq("instance.service", doc.instance.service),
        Filters.eq("instance.instance", doc.instance.instance),
        Filters.eq("instance.directory", doc.instance.directory))
      collections.State_ServiceStates.update(filters, _ => Some(doc))
    })).map(_ => ())
  }

  def getServicesState(distribution: Option[DistributionId], service: Option[ServiceId],
                       instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[DistributionServiceState]] = {
    val distributionArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val serviceArg = service.map { service => Filters.eq("instance.service", service) }
    val instanceArg = instance.map { instance => Filters.eq("instance.instance", instance) }
    val directoryArg = directory.map { directory => Filters.eq("instance.directory", directory) }
    val args = distributionArg ++ serviceArg ++ instanceArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ServiceStates.find(filters)
  }

  def getInstanceServiceStates(distribution: Option[DistributionId], service: Option[ServiceId],
                               instance: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[InstanceServiceState]] = {
    getServicesState(distribution, service, instance, directory).map(_.map(_.instance))
  }

  def addServiceLogs(distribution: DistributionId, service: ServiceId, task: Option[TaskId],
                     instance: InstanceId, process: ProcessId, directory: ServiceDirectory, logs: Seq[LogLine])(implicit log: Logger): Future[Unit] = {
    val documents = logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => { seq :+
      ServiceLogLine(distribution, service, task, instance, process, directory, line) })
    collections.State_ServiceLogs.insert(documents).map(_ => ())
  }

  def getLogDistributions()(implicit log: Logger): Future[Seq[DistributionId]] = {
    collections.State_ServiceLogs.distinctField[String]("distribution")
  }

  def getLogServices(distribution: DistributionId)
                    (implicit log: Logger): Future[Seq[ServiceId]] = {
    val distributionArg = Filters.eq("distribution", distribution)
    collections.State_ServiceLogs.distinctField[String]("service", distributionArg)
  }

  def getLogInstances(distribution: DistributionId, service: ServiceId)
                     (implicit log: Logger): Future[Seq[InstanceId]] = {
    val distributionArg = Filters.eq("distribution", distribution)
    val serviceArg = Filters.eq("service", service)
    val filters = Filters.and(Seq(distributionArg, serviceArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("instance", filters)
  }

  def getLogDirectories(distribution: DistributionId, service: ServiceId, instance: InstanceId)
                       (implicit log: Logger): Future[Seq[ServiceDirectory]] = {
    val distributionArg = Filters.eq("distribution", distribution)
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val filters = Filters.and(Seq(distributionArg, serviceArg, instanceArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("directory", filters)
  }

  def getLogProcesses(distribution: DistributionId, service: ServiceId, instance: InstanceId,
                      directory: ServiceDirectory)(implicit log: Logger): Future[Seq[ProcessId]] = {
    val distributionArg = Filters.eq("distribution", distribution)
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val directoryArg = Filters.eq("directory", instance)
    val filters = Filters.and(Seq(distributionArg, serviceArg, instanceArg, directoryArg).asJava)
    collections.State_ServiceLogs.distinctField[String]("process", filters)
  }

  def getServiceLogs(distribution: DistributionId, service: ServiceId, instance: InstanceId,
                     process: ProcessId, directory: ServiceDirectory,
                     fromSequence: Option[Long], toSequence: Option[Long],
                     fromTime: Option[Date], toTime: Option[Date],
                     findText: Option[String])
                    (implicit log: Logger): Future[Seq[SequencedLogLine]] = {
    val distributionArg = Filters.eq("distribution", distribution)
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val processArg = Filters.eq("process", process)
    val directoryArg = Filters.eq("directory", directory)
    val fromSequenceArg = fromSequence.map(sequence => Filters.gte("_id", sequence))
    val toSequenceArg = toSequence.map(sequence => Filters.lte("_id", sequence))
    val fromTimeArg = fromTime.map(time => Filters.gte("line.time", time))
    val toTimeArg = toTime.map(time => Filters.lte("line.time", time))
    val findTextArg = findText.map(text => Filters.text(text))
    val args = Seq(distributionArg, serviceArg, instanceArg, processArg, directoryArg) ++
      fromSequenceArg ++ toSequenceArg ++ fromTimeArg ++ toTimeArg ++ findTextArg
    val filters = Filters.and(args.asJava)
    collections.State_ServiceLogs.findSequenced(filters).map(_.map(line => SequencedLogLine(line.sequence, line.document.line)))
  }

  def getTaskLogs(task: TaskId)(implicit log: Logger): Future[Seq[SequencedLogLine]] = {
    val taskArg = Filters.eq("task", task)
    val filters = Filters.and(Seq(taskArg).asJava)
    collections.State_ServiceLogs.findSequenced(filters).map(_.map(line => SequencedLogLine(line.sequence, line.document.line)))
  }

  def subscribeServiceLogs(distribution: DistributionId, service: ServiceId,
                           instance: InstanceId, process: ProcessId, directory: ServiceDirectory,
                           fromSequence: Option[Long])(implicit log: Logger): Source[Action[Nothing, SequencedLogLine], NotUsed] = {
    val distributionArg = Filters.eq("distribution", distribution)
    val serviceArg = Filters.eq("service", service)
    val instanceArg = Filters.eq("instance", instance)
    val processArg = Filters.eq("process", process)
    val directoryArg = Filters.eq("directory", directory)
    val args = Seq(distributionArg, serviceArg, instanceArg, processArg, directoryArg)
    val filters = Filters.and(args.asJava)
    val source = collections.State_ServiceLogs.subscribe(filters, fromSequence)
      .filter(_.document.distribution == distribution)
      .filter(_.document.service == service)
      .filter(_.document.instance == instance)
      .filter(_.document.process == process)
      .filter(_.document.directory == directory)
      .takeWhile(!_.document.line.terminationStatus.isDefined, true)
      .map(line => Action(SequencedLogLine(line.sequence, line.document.line)))
      .buffer(250, OverflowStrategy.fail)
    source.mapMaterializedValue(_ => NotUsed)
  }

  def subscribeTaskLogs(task: TaskId, fromSequence: Option[Long])
                       (implicit log: Logger): Source[Action[Nothing, SequencedLogLine], NotUsed] = {
    val filters = Filters.eq("task", task)
    val source = collections.State_ServiceLogs.subscribe(filters, fromSequence)
      .filter(_.document.task.contains(task))
      .takeWhile(!_.document.line.terminationStatus.isDefined, true)
      .map(line => Action(SequencedLogLine(line.sequence, line.document.line)))
      .buffer(250, OverflowStrategy.fail)
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
    val serviceArg = service.map { service => Filters.eq("report.info.service", service) }
    val args = clientArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
    val sort = last.map { last => Sorts.descending("_id") }
    collections.State_FaultReportsInfo.find(filters, sort, last)
  }

  private def clearOldReports()(implicit log: Logger): Future[Unit] = {
    for {
      reports <- collections.State_FaultReportsInfo.findSequenced()
      result <- {
        val remainReports = reports
          .sortBy(_.document.report.info.time)
          .filter(_.document.report.info.time.getTime +
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
      val faultFile = directory.getFaultReportFile(report.document.report.faultId)
      faultFile.delete()
      collection.delete(Filters.and(Filters.eq("_id", report.sequence)))
    }).map(_ => Unit)
  }
}
