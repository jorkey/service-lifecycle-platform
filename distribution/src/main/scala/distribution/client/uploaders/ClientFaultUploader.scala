package distribution.client.uploaders

import java.io.{File, IOException}
import java.net.URL
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, failWith, onSuccess}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Keep, Source}
import akka.util.ByteString
import com.vyulabs.update.common.Common.{ClientName, ServiceName}
import com.vyulabs.update.distribution.client.ClientDistributionDirectory
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectoryClient, DeveloperDistributionWebPaths}
import com.vyulabs.update.utils.Utils
import org.slf4j.LoggerFactory

import scala.collection.immutable.Queue
import scala.util.{Failure, Success}

/**
  * Created by Andrei Kaplanov (akaplanov@vyulabs.com) on 16.12.19.
  * Copyright FanDate, Inc.
  */
class ClientFaultUploader(clientName: ClientName, dir: ClientDistributionDirectory, developerDirectoryUrl: URL)
                         (implicit materializer: Materializer)
      extends Thread with DeveloperDistributionWebPaths { self =>
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  private val developerDirectory = new DeveloperDistributionDirectoryClient(developerDirectoryUrl)

  private val maxFilesCount = 50
  private val maxServiceDirectoryCapacity = 1000 * 1024 * 1024

  private val faultsDirectory = dir.getFaultsDir()

  if (!faultsDirectory.exists() && !faultsDirectory.mkdir()) {
    log.error(s"Can't make directory ${faultsDirectory}")
  }

  private case class FaultFile(serviceName: ServiceName, file: File)

  private var downloadingFiles = Set.empty[File]

  private var faultsToUpload = Queue.empty[FaultFile]
  private var stopping = false

  queueLastHourFiles()

  def close(): Unit = {
    self.synchronized {
      stopping = true
      notify()
    }
    join()
  }

  def receiveFault(serviceName: ServiceName, fileName: String, source: Source[ByteString, Any]): Route = {
    log.info(s"Receive fault file ${fileName}")
    val serviceDir = new File(faultsDirectory, serviceName.toString)
    if (!serviceDir.exists() && !serviceDir.mkdir()) {
      return failWith(new IOException(s"Can't make directory ${serviceDir}"))
    }
    val nextIndex = calculateNextIndex(serviceDir)
    val file = new File(serviceDir, "%06d".format(nextIndex) + "-" + fileName)
    self.synchronized { downloadingFiles += file }
    val sink = FileIO.toPath(file.toPath)
    val result = source.runWith(sink)
    onSuccess(result) { result =>
      result.status match {
        case Success(_) =>
          self.synchronized {
            downloadingFiles -= file
            faultsToUpload = faultsToUpload.enqueue(FaultFile(serviceName, file))
            val notDeleteFiles = faultsToUpload.map(_.file).toSet ++ downloadingFiles
            Utils.maybeDeleteExcessFiles(serviceDir, maxFilesCount, notDeleteFiles)
            Utils.maybeFreeSpace(serviceDir, maxServiceDirectoryCapacity, notDeleteFiles)
            notify()
          }
          complete(StatusCodes.OK)
        case Failure(ex) =>
          self.synchronized {
            downloadingFiles -= file
          }
          return failWith(ex)
      }
    }
  }

  private def calculateNextIndex(dir: File): Int = {
    var max = 0
    for (file <- dir.listFiles()) {
      val name = file.getName
      val index = name.indexOf('-')
      if (index != -1) {
        try {
          val num = name.substring(0, index).toInt
          if (num > max) {
            max = num
          }
        } catch {
          case _: NumberFormatException =>
        }
      }
    }
    max + 1
  }

  private def queueLastHourFiles(): Unit = {
    for (serviceDir <- faultsDirectory.listFiles()) {
      val serviceName = serviceDir.getName
      for (faultFile <- serviceDir.listFiles()) {
        if ((System.currentTimeMillis() - faultFile.lastModified()) < TimeUnit.MINUTES.toMillis(15)) {
          log.info(s"Fault file ${faultFile} will be uploaded")
          faultsToUpload = faultsToUpload.enqueue(FaultFile(serviceName, faultFile))
        }
      }
    }
  }

  override def run(): Unit = {
    log.info("Fault uploader started")
    try {
      var lastClearTime = 0L
      while (true) {
        val faultFile = self.synchronized {
          if (stopping) {
            return
          }
          if (faultsToUpload.isEmpty) {
            wait(TimeUnit.DAYS.toMillis(1))
            if (stopping) {
              return
            }
          }
          if (!faultsToUpload.isEmpty) {
            val entry = faultsToUpload.dequeue
            faultsToUpload = entry._2
            Some(entry._1)
          } else {
            None
          }
        }
        for (faultFile <- faultFile) {
          log.info(s"Uploading fault file ${faultFile.file}")
          if (!developerDirectory.uploadServiceFault(faultFile.serviceName, faultFile.file)) {
            log.error(s"Can't upload service ${faultFile.serviceName} fault file ${faultFile.file}")
          }
        }
        if (System.currentTimeMillis() - lastClearTime >= TimeUnit.DAYS.toMillis(1)) {
          for (serviceDir <- faultsDirectory.listFiles()) {
            for (faultFile <- serviceDir.listFiles()) {
              if ((System.currentTimeMillis() - faultFile.lastModified()) >= TimeUnit.DAYS.toMillis(30)) {
                faultFile.delete()
              }
            }
          }
          lastClearTime = System.currentTimeMillis()
        }
      }
    } catch {
      case ex: Exception =>
        log.error(s"Fault uploader thread is failed", ex)
    }
  }
}
