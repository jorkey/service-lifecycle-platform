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

trait StateUtils extends DistributionConsumersUtils with SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def setInstalledDesiredVersions(distributionName: DistributionName, desiredVersions: Seq[ClientDesiredVersion])(implicit log: Logger): Future[Unit] = {
    val clientArg = Filters.eq("distributionName", distributionName)
    collections.State_InstalledDesiredVersions.update(clientArg, _ => Some(InstalledDesiredVersions(distributionName, desiredVersions))).map(_ => ())
  }

  def getInstalledDesiredVersions(distributionName: DistributionName, serviceNames: Set[ServiceName] = Set.empty)(implicit log: Logger): Future[Seq[ClientDesiredVersion]] = {
    val clientArg = Filters.eq("distributionName", distributionName)
    collections.State_InstalledDesiredVersions.find(clientArg).map(_.headOption.map(_.versions).getOrElse(Seq.empty[ClientDesiredVersion]))
      .map(_.filter(v => serviceNames.isEmpty || serviceNames.contains(v.serviceName)).sortBy(_.serviceName))
  }

  def getInstalledDesiredVersion(distributionName: DistributionName, serviceName: ServiceName)(implicit log: Logger): Future[Option[ClientDesiredVersion]] = {
    getInstalledDesiredVersions(distributionName, Set(serviceName)).map(_.headOption)
  }

  def setTestedVersions(distributionName: DistributionName, desiredVersions: Seq[DeveloperDesiredVersion])(implicit log: Logger): Future[Unit] = {
    for {
      clientConfig <- getDistributionConsumerInfo(distributionName)
      testedVersions <- getTestedVersions(clientConfig.consumerProfile)
      result <- {
        val testRecord = TestSignature(distributionName, new Date())
        val testSignatures = testedVersions match {
          case Some(testedVersions) if testedVersions.versions.equals(desiredVersions) =>
            testedVersions.signatures :+ testRecord
          case _ =>
            Seq(testRecord)
        }
        val newTestedVersions = TestedDesiredVersions(clientConfig.consumerProfile, desiredVersions, testSignatures)
        val profileArg = Filters.eq("profileName", clientConfig.consumerProfile)
        collections.State_TestedVersions.update(profileArg, _ => Some(newTestedVersions)).map(_ => ())
      }
    } yield result
  }

  def getTestedVersions(profileName: ConsumerProfileName)(implicit log: Logger): Future[Option[TestedDesiredVersions]] = {
    val profileArg = Filters.eq("profileName", profileName)
    collections.State_TestedVersions.find(profileArg).map(_.headOption)
  }

  def setSelfServiceStates(states: Seq[DirectoryServiceState])(implicit log: Logger): Future[Unit] = {
    setServiceStates(config.distributionName, states.map(state => InstanceServiceState(config.instanceId, state.serviceName, state.directory, state.state)))
  }

  def setServiceStates(distributionName: DistributionName, instanceStates: Seq[InstanceServiceState])(implicit log: Logger): Future[Unit] = {
    val documents = instanceStates.foldLeft(Seq.empty[DistributionServiceState])((seq, state) => seq :+ DistributionServiceState(distributionName, state))
    Future.sequence(documents.map(doc => {
      val filters = Filters.and(
        Filters.eq("distributionName", distributionName),
        Filters.eq("instance.serviceName", doc.instance.serviceName),
        Filters.eq("instance.instanceId", doc.instance.instanceId),
        Filters.eq("instance.directory", doc.instance.directory))
      collections.State_ServiceStates.update(filters, _ => Some(doc))
    })).map(_ => ())
  }

  def getServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName],
                       instanceId: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[DistributionServiceState]] = {
    val distributionArg = distributionName.map { distribution => Filters.eq("distributionName", distribution) }
    val serviceArg = serviceName.map { service => Filters.eq("instance.serviceName", service) }
    val instanceIdArg = instanceId.map { instanceId => Filters.eq("instance.instanceId", instanceId) }
    val directoryArg = directory.map { directory => Filters.eq("instance.directory", directory) }
    val args = distributionArg ++ serviceArg ++ instanceIdArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    collections.State_ServiceStates.find(filters)
  }

  def getInstanceServicesState(distributionName: Option[DistributionName], serviceName: Option[ServiceName],
                      instanceId: Option[InstanceId], directory: Option[ServiceDirectory])(implicit log: Logger): Future[Seq[InstanceServiceState]] = {
    getServicesState(distributionName, serviceName, instanceId, directory).map(_.map(_.instance))
  }

  def addServiceLogs(distributionName: DistributionName, serviceName: ServiceName, taskId: Option[TaskId],
                     instanceId: InstanceId, processId: ProcessId, directory: ServiceDirectory, logs: Seq[LogLine])(implicit log: Logger): Future[Unit] = {
    val documents = logs.foldLeft(Seq.empty[ServiceLogLine])((seq, line) => { seq :+
      ServiceLogLine(distributionName, serviceName, taskId, instanceId, processId, directory, line) })
    collections.State_ServiceLogs.insert(documents).map(_ => ())
  }

  def getServiceLogs(distributionName: DistributionName, serviceName: ServiceName, instanceId: InstanceId,
                     processId: ProcessId, directory: ServiceDirectory, fromSequence: Option[Long])
                    (implicit log: Logger): Future[Seq[Sequenced[ServiceLogLine]]] = {
    val distributionArg = Filters.eq("distributionName", distributionName)
    val serviceArg = Filters.eq("serviceName", serviceName)
    val instanceArg = Filters.eq("instanceId", instanceId)
    val processArg = Filters.eq("processId", processId)
    val directoryArg = Filters.eq("directory", directory)
    val sequenceArg = fromSequence.map(sequence => Filters.gte("_id", sequence))
    val args = Seq(distributionArg, serviceArg, instanceArg, processArg, directoryArg) ++ sequenceArg
    val filters = Filters.and(args.asJava)
    collections.State_ServiceLogs.findSequenced(filters)
  }

  def getTaskLogs(taskId: TaskId, fromSequence: Option[Long])(implicit log: Logger): Future[Seq[Sequenced[ServiceLogLine]]] = {
    val taskArg = Filters.eq("taskId", taskId)
    val sequenceArg = fromSequence.map(sequence => Filters.gte("_id", sequence))
    val filters = Filters.and((Seq(taskArg) ++ sequenceArg).asJava)
    collections.State_ServiceLogs.findSequenced(filters)
  }

  def subscribeServiceLogs(distributionName: DistributionName, serviceName: ServiceName,
                           instanceId: InstanceId, processId: ProcessId, directory: ServiceDirectory,
                           fromSequence: Option[Long])(implicit log: Logger): Source[Action[Nothing, SequencedServiceLogLine], NotUsed] = {
    val distributionArg = Filters.eq("distributionName", distributionName)
    val serviceArg = Filters.eq("serviceName", serviceName)
    val instanceArg = Filters.eq("instanceId", instanceId)
    val processArg = Filters.eq("processId", processId)
    val directoryArg = Filters.eq("directory", directory)
    val args = Seq(distributionArg, serviceArg, instanceArg, processArg, directoryArg)
    val filters = Filters.and(args.asJava)
    val source = collections.State_ServiceLogs.subscribe(filters, fromSequence)
      .filter(_.document.distributionName == distributionName)
      .filter(_.document.serviceName == serviceName)
      .filter(_.document.instanceId == instanceId)
      .filter(_.document.processId == processId)
      .filter(_.document.directory == directory)
      .via(NippleFlow.takeWhile(!_.document.line.terminationStatus.isDefined))
      .map(line => Action(SequencedServiceLogLine(line.sequence, line.document)))
      .buffer(250, OverflowStrategy.fail)
    source.mapMaterializedValue(_ => NotUsed)
  }

  def subscribeTaskLogs(taskId: TaskId, fromSequence: Option[Long])
                       (implicit log: Logger): Source[Action[Nothing, SequencedServiceLogLine], NotUsed] = {
    val filters = Filters.eq("taskId", taskId)
    val source = collections.State_ServiceLogs.subscribe(filters, fromSequence)
      .filter(_.document.taskId.contains(taskId))
      .via(NippleFlow.takeWhile(!_.document.line.terminationStatus.isDefined))
      .map(line => Action(SequencedServiceLogLine(line.sequence, line.document)))
      .buffer(250, OverflowStrategy.fail)
    source.mapMaterializedValue(_ => NotUsed)
  }

  def testSubscription()(implicit log: Logger): Source[Action[Nothing, String], NotUsed] = {
    Source.tick(FiniteDuration(1, TimeUnit.SECONDS), FiniteDuration(1, TimeUnit.SECONDS), Action("line")).mapMaterializedValue(_ => NotUsed).take(5)
  }

  def addServiceFaultReportInfo(distributionName: DistributionName, report: ServiceFaultReport)(implicit log: Logger): Future[Unit] = {
    for {
      result <- collections.State_FaultReportsInfo.insert(DistributionFaultReport(distributionName, report)).map(_ => ())
      _ <- clearOldReports()
    } yield result
  }

  def getDistributionFaultReportsInfo(distributionName: Option[DistributionName], serviceName: Option[ServiceName],
                                      last: Option[Int])(implicit log: Logger)
      : Future[Seq[DistributionFaultReport]] = {
    val clientArg = distributionName.map { client => Filters.eq("distributionName", client) }
    val serviceArg = serviceName.map { service => Filters.eq("report.info.serviceName", service) }
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
          .sortBy(_.document.report.info.date)
          .filter(_.document.report.info.date.getTime +
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
