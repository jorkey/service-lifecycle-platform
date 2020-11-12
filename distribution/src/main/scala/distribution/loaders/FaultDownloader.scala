package distribution.loaders

import java.io.{File}
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
import com.vyulabs.update.common.Common.{ClientName, FaultId}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{ClientFaultReport, FaultInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IoUtils, ZipUtils}
import distribution.graphql.utils.GetUtils
import distribution.mongo.{DatabaseCollections, FaultReportDocument}
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
                             protected val filesLocker: SmartFilesLocker) extends GetUtils { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val expirationPeriod = TimeUnit.DAYS.toMillis(30)
  private val maxClientServiceReportsCount = 100

  def receiveFault(faultId: FaultId, clientName: ClientName, source: Source[ByteString, Any]): Route = {
    log.info(s"Receive fault file from client ${clientName}")
    val file = dir.getFaultReportFile(faultId)
    val sink = FileIO.toPath(file.toPath)
    val result = source.runWith(sink)
    onSuccess(result) { result =>
      result.status match {
        case Success(_) =>
          complete(Future.apply(processFaultReportTask(faultId, clientName, file)).map(_ => OK))
        case Failure(ex) =>
          failWith(ex)
      }
    }
  }

  private def processFaultReportTask(faultId: FaultId, clientName: ClientName, file: File): Unit = {
    implicit val log = LoggerFactory.getLogger(getClass)

    if (file.getName.endsWith(".zip")) {
      val faultDir = Files.createTempDirectory("fault").toFile
      if (ZipUtils.unzip(file, faultDir)) {
        file.delete()
        val faultInfoFile = new File(faultDir, Common.FaultInfoFileName)
        parseJsonFileWithLock[FaultInfo](faultInfoFile).foreach { faultInfo =>
          faultInfo match {
            case Some(faultInfo) =>
              for {
                collection <- collections.State_FaultReports
                id <- collections.getNextSequence(collection.getName())
                result <- collection.insert(FaultReportDocument(id, ClientFaultReport(faultId, clientName, faultInfo, IoUtils.listFiles(faultDir))))
              } yield result
            case None =>
              log.warn(s"No file ${Common.FaultInfoFileName} in the fault report ${faultDir}")
          }
        }
      } else {
        log.error(s"Can't make directory ${file}")
      }
    }
    (for   {
      collection <- collections.State_FaultReports
      reports <- collection.find(Filters.eq("client", clientName))
    } yield (collection, reports)).foreach { case (collection, reports) => {
      val remainReports = reports
        .sortBy(_.fault.info.date)
        .filter(_.fault.info.date.getTime + expirationPeriod >= System.currentTimeMillis())
        .take(maxClientServiceReportsCount)
      (reports.toSet -- remainReports.toSet).foreach { report =>
        collection.delete(Filters.and(
          Filters.eq("report.reportId", report.fault.faultId)))
        val faultFile = dir.getFaultReportFile(report.fault.faultId)
        faultFile.delete()
      }
    }}
  }
}
