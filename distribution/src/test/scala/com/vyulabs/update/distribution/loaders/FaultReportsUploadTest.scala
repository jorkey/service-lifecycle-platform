package com.vyulabs.update.distribution.loaders

import java.io.{File, IOException}
import java.util.Date

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.FaultReportDocument

import scala.concurrent.{Future, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  case class FileUploadRequest(path: String, file: File)

  var requestPromise = Promise[FileUploadRequest]
  var result = Future.successful()

  def fileUploadRequest(path: String, file: File): Future[Unit] = {
    requestPromise.success(FileUploadRequest(path, file))
    result
  }

  it should "upload fault reports" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 1,
      (_, _) => Future.failed(new IOException("Not expected")),
      fileUploadRequest)
    uploader.start()

    val date = new Date()
    val report = DistributionFaultReport("fault1", "client1", FaultInfo(date, "instance1", "directory", "service1", "profile1",
      ServiceState(date, None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report))))
    requestPromise = Promise[FileUploadRequest]
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, "/faults/fault1-fault.zip")))(result(requestPromise.future))

    uploader.stop()
    result(collections.State_FaultReports.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }
}