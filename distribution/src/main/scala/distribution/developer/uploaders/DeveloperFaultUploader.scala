package distribution.developer.uploaders

import java.io.{File, IOException}
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith, onSuccess}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.utils.Utils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 18.12.19.
  * Copyright FanDate, Inc.
  */
class DeveloperFaultUploader(dir: DeveloperDistributionDirectory)
                            (implicit materializer: Materializer) extends DeveloperDistributionWebPaths { self =>
  implicit val log = LoggerFactory.getLogger(this.getClass)

  private val directory = dir.getFaultsDir()

  private val maxClientServiceReportsCount = 25
  private val maxClientServiceDirectoryCapacity = 1024 * 1024 * 1024

  private var downloadingFiles = Set.empty[File]
  private val expirationPeriod = TimeUnit.DAYS.toMillis(30)

  def receiveFault(clientName: ClientName, serviceName: ServiceName, fileName: String, source: Source[ByteString, Any]): Route = {
    log.info(s"Receive fault file ${fileName}")
    val serviceDir = new File(directory, serviceName.toString)
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
          new ProcessFaultReportTask(clientDir, file).start()
          complete(StatusCodes.OK)
        case Failure(ex) =>
          return failWith(ex)
      }
    }
  }

  class ProcessFaultReportTask(dir: File, file: File) extends Thread {
    implicit val log = LoggerFactory.getLogger(getClass)

    override def run(): Unit = {
      if (file.getName.endsWith(".zip")) {
        val faultDir = new File(dir, file.getName.substring(0, file.getName.length - 4))
        if (faultDir.exists()) {
          Utils.deleteFileRecursively(faultDir)
        }
        if (faultDir.mkdir()) {
          if (Utils.unzip(file, dir)) {
            file.delete()
          }
        } else {
          log.error(s"Can't make directory ${faultDir}")
        }
      }
      self.synchronized {
        Utils.maybeDeleteOldFiles(dir, System.currentTimeMillis() - expirationPeriod, downloadingFiles)
        Utils.maybeDeleteExcessFiles(dir, maxClientServiceReportsCount, downloadingFiles)
        Utils.maybeFreeSpace(dir, maxClientServiceDirectoryCapacity, downloadingFiles)
        downloadingFiles -= file
      }
    }
  }
}
