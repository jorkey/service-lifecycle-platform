package com.vyulabs.update.distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.common.Common._
import com.vyulabs.update.common.config.DistributionConfig
import com.vyulabs.update.common.distribution.server.DistributionDirectory
import com.vyulabs.update.common.info._
import com.vyulabs.update.distribution.mongo._
import org.bson.BsonDocument
import org.slf4j.Logger

import java.util.Date
import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait FaultsUtils extends SprayJsonSupport {
  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val directory: DistributionDirectory
  protected val collections: DatabaseCollections

  protected val config: DistributionConfig

  def addServiceFaultReportInfo(distribution: DistributionId, report: ServiceFaultReport)(implicit log: Logger): Future[Unit] = {
    for {
      result <- collections.Faults_ReportsInfo.insert(DistributionFaultReport(
        distribution = distribution,
        fault = report.fault,
        info = report.info,
        files = report.files)).map(_ => ())
      _ <- clearOldReports()
    } yield result
  }

  def getFaultDistributions()(implicit log: Logger): Future[Seq[DistributionId]] = {
    collections.Faults_ReportsInfo.distinctField[String]("distribution")
  }

  def getFaultServices(distribution: Option[DistributionId])(implicit log: Logger): Future[Seq[ServiceId]] = {
    val distributionArg = distribution.map(Filters.eq("distribution", _))
    val filters = distributionArg.getOrElse(new BsonDocument())
    collections.Faults_ReportsInfo.distinctField[String](
      "info.service", filters)
  }

  def getFaultsStartTime(distribution: Option[ServiceId], service: Option[ServiceId])
                       (implicit log: Logger): Future[Option[Date]] = {
    val distributionArg = distribution.map(Filters.eq("distribution", _))
    val serviceArg = service.map(Filters.eq("service", _))
    val args = distributionArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Sorts.ascending("info.time")
    collections.Faults_ReportsInfo.find(filters, Some(sort), Some(1)).map(_.headOption.map(_.info.time))
  }

  def getFaultsEndTime(distribution: Option[ServiceId], service: Option[ServiceId])
                       (implicit log: Logger): Future[Option[Date]] = {
    val distributionArg = distribution.map(Filters.eq("distribution", _))
    val serviceArg = service.map(Filters.eq("service", _))
    val args = distributionArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Sorts.descending("info.time")
    collections.Faults_ReportsInfo.find(filters, Some(sort), Some(1)).map(_.headOption.map(_.info.time))
  }

  def getFaults(distribution: Option[DistributionId], fault: Option[FaultId],
                service: Option[ServiceId], fromTime: Option[Date], toTime: Option[Date],
                limit: Option[Int])(implicit log: Logger)
      : Future[Seq[DistributionFaultReport]] = {
    val distributionArg = distribution.map { distribution => Filters.eq("distribution", distribution) }
    val faultArg = fault.map(fault => Filters.lte("fault", fault))
    val serviceArg = service.map { service => Filters.eq("info.service", service) }
    val fromTimeArg = fromTime.map(time => Filters.gte("info.time", time))
    val toTimeArg = toTime.map(time => Filters.lte("info.time", time))
    val args = faultArg ++ distributionArg ++ serviceArg ++ fromTimeArg ++ toTimeArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    val sort = Some(Sorts.descending("_sequence"))
    collections.Faults_ReportsInfo.find(filters, sort, limit)
  }

  private def clearOldReports()(implicit log: Logger): Future[Unit] = {
    for {
      reports <- collections.Faults_ReportsInfo.findSequenced()
      result <- {
        val remainReports = reports
          .sortBy(_.document.info.time)
          .filter(_.document.info.time.getTime +
            config.faultReports.expirationTimeout.toMillis >= System.currentTimeMillis())
          .takeRight(config.faultReports.maxReportsCount)
        deleteReports(collections.Faults_ReportsInfo, reports.toSet -- remainReports.toSet)
      }
    } yield result
  }

  private def deleteReports(collection: SequencedCollection[DistributionFaultReport], reports: Set[Sequenced[DistributionFaultReport]])
                           (implicit log: Logger): Future[Unit] = {
    Future.sequence(reports.map { report =>
      log.debug(s"Delete fault report ${report.sequence}")
      val faultFile = directory.getFaultReportFile(report.document.fault)
      faultFile.delete()
      collection.delete(Filters.eq("_sequence", report.sequence))
    }).map(_ => Unit)
  }
}
