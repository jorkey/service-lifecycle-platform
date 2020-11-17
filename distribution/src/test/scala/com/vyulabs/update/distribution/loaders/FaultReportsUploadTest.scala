package com.vyulabs.update.distribution.loaders

import java.io.IOException
import java.util.Date

import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{ClientFaultReport, ClientServiceState, DirectoryServiceState, FaultInfo, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.{FaultReportDocument, ServiceStateDocument}
import spray.json.{JsValue, enrichAny}

import scala.concurrent.{Future, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  case class FileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]])

  var requestPromise = Promise[FileUploadRequest]
  var result = Future.successful()

  def fileUploadRequest(path: String, fileName: String, length: Long, source: Source[ByteString, Future[IOResult]]): Future[Unit] = {
    requestPromise.success(FileUploadRequest(path, fileName, length, source))
    result
  }

  it should "upload fault reports" in {
    val uploader = new StateUploader(collections, distributionDir, 1,
      (_, _) => Future.failed(new IOException("Not expected")),
      fileUploadRequest)
    uploader.start()

    val date = new Date()
    val report = ClientFaultReport("fault1", "client1", FaultInfo(date, "instance1", "directory", "service1", "profile1",
      ServiceState(date, None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report))))
    requestPromise = Promise[FileUploadRequest]
    val request1 = result(requestPromise.future)

    uploader.stop()
    result(collections.State_FaultReports.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }
}