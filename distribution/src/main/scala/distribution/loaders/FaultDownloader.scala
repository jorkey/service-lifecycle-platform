package distribution.loaders

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{DistributionName, FaultId}
import com.vyulabs.update.distribution.DistributionDirectory
import distribution.mongo.{DatabaseCollections, FaultReportDocument, MongoDbCollection}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class FaultDownloader(collections: DatabaseCollections, dir: DistributionDirectory)
                     (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val expirationPeriod = TimeUnit.DAYS.toMillis(30)
  private val maxFaultReportsCount = 100

  def receiveFault(faultId: FaultId, distributionName: DistributionName, source: Source[ByteString, Any]): Route = {
    log.info(s"Receive fault report file from client ${distributionName}")
    val file = dir.getFaultReportFile(faultId)
    val sink = FileIO.toPath(file.toPath)
    val result = source.runWith(sink)
    onSuccess(result) { result =>
      result.status match {
        case Success(_) =>
          complete(clearOldReports().map(_ => OK))
        case Failure(ex) =>
          failWith(ex)
      }
    }
  }

  private def clearOldReports(): Future[Unit] = {
    for {
      collection <- collections.State_FaultReportsInfo
      reports <- collection.find()
      result <- {
        val remainReports = reports
          .sortBy(_.fault.report.info.date)
          .filter(_.fault.report.info.date.getTime + expirationPeriod >= System.currentTimeMillis())
          .take(maxFaultReportsCount)
        Future(deleteReports(collection, reports.toSet -- remainReports.toSet))
      }
    } yield result
  }

  private def deleteReports(collection: MongoDbCollection[FaultReportDocument], reports: Set[FaultReportDocument]): Unit = {
    reports.foreach { report =>
      collection.delete(Filters.and(Filters.eq("fault.report.reportId", report.fault.report.faultId)))
      val faultFile = dir.getFaultReportFile(report.fault.report.faultId)
      faultFile.delete()
    }
  }
}