package distribution.uploaders

import java.io.{File, IOException}
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
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
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
class DeveloperFaultUploader(collections: DatabaseCollections,
                             protected val dir: DistributionDirectory)
                            (implicit protected val system: ActorSystem,
                             protected val materializer: Materializer,
                             protected val executionContext: ExecutionContext,
                             protected val filesLocker: SmartFilesLocker) extends GetUtils { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val directory: File = null // TODO graphql dir.getFaultsDir()

  private val expirationPeriod = TimeUnit.DAYS.toMillis(30)
  private val maxClientServiceReportsCount = 100

  def receiveFault(clientName: ClientName, serviceName: ServiceName, fileName: String,
                   source: Source[ByteString, Any]): Route = {
    log.info(s"Receive fault file ${fileName} from client ${clientName}")
    val serviceDir = new File(directory, serviceName)
    if (!serviceDir.exists() && !serviceDir.mkdir()) {
      return failWith(new IOException(s"Can't make directory ${serviceDir}"))
    }
    val clientDir = new File(serviceDir, clientName)
    if (!clientDir.exists() && !clientDir.mkdir()) {
      return failWith(new IOException(s"Can't make directory ${clientDir}"))
    }
    val file = new File(clientDir, fileName)
    val sink = FileIO.toPath(file.toPath)
    val result = source.runWith(sink)
    onSuccess(result) { result =>
      result.status match {
        case Success(_) =>
          complete(Future.apply(processFaultReportTask(clientName, serviceName, file)).map(_ => OK))
        case Failure(ex) =>
          failWith(ex)
      }
    }
  }

  private def processFaultReportTask(clientName: ClientName, serviceName: ServiceName, file: File): Unit = {
    implicit val log = LoggerFactory.getLogger(getClass)

    val clientDir = new File(new File(directory, serviceName), clientName)
    if (file.getName.endsWith(".zip")) {
      val dirName = file.getName.substring(0, file.getName.length - 4)
      val faultDir = new File(clientDir, dirName)
      if (faultDir.exists()) {
        IoUtils.deleteFileRecursively(faultDir)
      }
      if (faultDir.mkdir()) {
        if (ZipUtils.unzip(file, faultDir)) {
          file.delete()
          val faultInfoFile = new File(faultDir, Common.FaultInfoFileName)
          parseJsonFileWithLock[FaultInfo](faultInfoFile).foreach { faultInfo =>
            faultInfo match {
              case Some(faultInfo) =>
                for {
                  collection <- collections.State_FaultReports
                  result <- collection.insert(FaultReportDocument(ClientFaultReport(clientName, dirName,
                    IoUtils.listFiles(faultDir), faultInfo)))
                } yield result
              case None =>
                log.warn(s"No file ${Common.FaultInfoFileName} in the fault report ${faultDir}")
            }
          }
        }
      } else {
        log.error(s"Can't make directory ${file}")
      }
    }
    (for {
      collection <- collections.State_FaultReports
      reports <- collection.find(Filters.eq("client", clientName))
    } yield (collection, reports)).foreach { case (collection, reports) => {
      val remainReports = reports
        .sortBy(_.report.faultInfo.date)
        .filter(_.report.faultInfo.date.getTime + expirationPeriod >= System.currentTimeMillis())
        .take(maxClientServiceReportsCount)
      (reports.toSet -- remainReports.toSet).foreach { report =>
        collection.delete(Filters.and(
          Filters.eq("clientName", report.report.clientName),
          Filters.eq("directoryName", report.report.reportDirectory)))
        val faultDir = new File(clientDir, report.report.reportDirectory)
        IoUtils.deleteFileRecursively(faultDir)
      }
    }}
  }
}
