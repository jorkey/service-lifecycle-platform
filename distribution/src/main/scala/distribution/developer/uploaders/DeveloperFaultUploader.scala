package distribution.developer.uploaders

import java.io.{File, IOException}
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.info.{ClientFaultInfo, FaultInfo}
import com.vyulabs.update.lock.SmartFilesLocker
import com.vyulabs.update.utils.{IOUtils, ZipUtils}
import distribution.mongo.MongoDbCollection
import distribution.utils.GetUtils
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class DeveloperFaultUploader(mongoDbCollection: MongoDbCollection[ClientFaultInfo],
                             protected val dir: DeveloperDistributionDirectory)
                            (implicit protected val system: ActorSystem,
                             protected val materializer: Materializer,
                             protected val executionContext: ExecutionContext,
                             protected val filesLocker: SmartFilesLocker)
        extends DeveloperDistributionWebPaths with GetUtils { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val directory = dir.getFaultsDir()

  private val maxClientServiceReportsCount = 100
  private val maxClientServiceDirectoryCapacity = 1024 * 1024 * 1024

  private var downloadingFiles = Set.empty[File]
  private val expirationPeriod = TimeUnit.DAYS.toMillis(30)

  def receiveFault(clientName: ClientName, serviceName: ServiceName, fileName: String, source: Source[ByteString, Any]): Route = {
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
    self.synchronized { downloadingFiles += file }
    val sink = FileIO.toPath(file.toPath)
    val result = source.runWith(sink)
    onSuccess(result) { result =>
      result.status match {
        case Success(_) =>
          complete(Future.apply(processFaultReportTask(clientName, clientDir, file)).map(_ => OK))
        case Failure(ex) =>
          failWith(ex)
      }
    }
  }

  private def processFaultReportTask(clientName: ClientName, dir: File, file: File): Unit = {
    implicit val log = LoggerFactory.getLogger(getClass)

    if (file.getName.endsWith(".zip")) {
      val faultDir = new File(dir, file.getName.substring(0, file.getName.length - 4))
      if (faultDir.exists()) {
        IOUtils.deleteFileRecursively(faultDir)
      }
      if (faultDir.mkdir()) {
        if (ZipUtils.unzip(file, faultDir)) {
          file.delete()
          val faultInfoFile = new File(faultDir, Common.FaultInfoFileName)
          parseJsonFileWithLock[FaultInfo](faultInfoFile).foreach { faultInfo =>
            faultInfo match {
              case Some(faultInfo) =>
                mongoDbCollection.insert(ClientFaultInfo(clientName, faultInfo))
              case None =>
                log.warn(s"No file ${Common.FaultInfoFileName} in the fault report ${faultDir}")
            }
          }
        }
      } else {
        log.error(s"Can't make directory ${file}")
      }
    }
    self.synchronized {
      IOUtils.maybeDeleteOldFiles(dir, System.currentTimeMillis() - expirationPeriod, downloadingFiles)
      IOUtils.maybeDeleteExcessFiles(dir, maxClientServiceReportsCount, downloadingFiles)
      IOUtils.maybeFreeSpace(dir, maxClientServiceDirectoryCapacity, downloadingFiles)
      downloadingFiles -= file
    }
  }
}
