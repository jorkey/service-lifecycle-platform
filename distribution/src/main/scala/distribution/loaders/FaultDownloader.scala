package distribution.loaders

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{DistributionName, FaultId}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{ClientFaultReport, FaultInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, Utils, ZipUtils}
import distribution.mongo.{DatabaseCollections, FaultReportDocument, MongoDbCollection}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class FaultDownloader(collections: DatabaseCollections,
                      protected val dir: DistributionDirectory)
                     (implicit protected val system: ActorSystem,
                      protected val materializer: Materializer,
                      protected val executionContext: ExecutionContext,
                      protected val filesLocker: SmartFilesLocker) { self =>
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
          val res = for {
            _ <- Future(processFaultReport(faultId, distributionName, file))
            _ <- clearOldReports()
          } yield result
          complete(res.map(_ => OK))
        case Failure(ex) =>
          failWith(ex)
      }
    }
  }

  private def processFaultReport(faultId: FaultId, distributionName: DistributionName, file: File): Unit = {
    implicit val log = LoggerFactory.getLogger(getClass)

    val tmpDir = Files.createTempDirectory("fault").toFile
    if (ZipUtils.unzip(file, tmpDir)) {
      val faultInfoFile = new File(tmpDir, Common.FaultInfoFileName)
      IoUtils.readFileToJson[FaultInfo](faultInfoFile) match {
        case Some(faultInfo) =>
          for {
            collection <- collections.State_FaultReports
            id <- collections.getNextSequence(collection.getName())
            result <- collection.insert(FaultReportDocument(id, ClientFaultReport(faultId, distributionName, faultInfo, IoUtils.listFiles(tmpDir))))
          } yield result
        case None =>
          log.warn(s"No file ${Common.FaultInfoFileName} in the fault report ${tmpDir}")
      }
    } else {
      log.error(s"Can't unzip ${file}")
    }
  }

  private def clearOldReports(): Future[Unit] = {
    for {
      collection <- collections.State_FaultReports
      reports <- collection.find()
      result <- {
        val remainReports = reports
          .sortBy(_.fault.info.date)
          .filter(_.fault.info.date.getTime + expirationPeriod >= System.currentTimeMillis())
          .take(maxFaultReportsCount)
        Future(deleteReports(collection, reports.toSet -- remainReports.toSet))
      }
    } yield result
  }

  private def deleteReports(collection: MongoDbCollection[FaultReportDocument], reports: Set[FaultReportDocument]): Unit = {
    reports.foreach { report =>
      collection.delete(Filters.and(Filters.eq("report.reportId", report.fault.faultId)))
      val faultFile = dir.getFaultReportFile(report.fault.faultId)
      faultFile.delete()
    }
  }
}