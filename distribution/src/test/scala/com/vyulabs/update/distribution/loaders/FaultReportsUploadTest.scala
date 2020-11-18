package com.vyulabs.update.distribution.loaders

import java.io.{File, IOException}
import java.util.Date

import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.{FaultReportDocument, UploadStatus, UploadStatusDocument}

import scala.concurrent.{Future, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  case class FileUploadRequest(path: String, file: File)

  var promise = Promise[FileUploadRequest]
  var result = Future.successful()
  var l = 0

  val date = new Date()

  def fileUploadRequest(path: String, file: File): Future[Unit] = {
    synchronized {
      log.info(s"Upload request path ${path} file ${file}")
      promise.success(FileUploadRequest(path, file))
      result
    }
  }

  it should "upload fault reports" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 1,
      (_, _) => Future.failed(new IOException("Not expected")), fileUploadRequest)
    uploader.start()

    val report = DistributionFaultReport("fault1", "client1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report))))
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, "/faults/fault1-fault.zip")))(result(promise.future))

    Thread.sleep(100)
    uploader.stop()

    result(collections.State_FaultReports.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 2,
      (_, _) => Future.failed(new IOException("Not expected")), fileUploadRequest)
    promise = Promise[FileUploadRequest]
    result = Future.failed(new IOException("upload error"))
    uploader.start()

    val report1 = DistributionFaultReport("fault1", "client1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report1))))
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, "/faults/fault1-fault.zip")))(result(promise.future))

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.faultReports", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.faultReports")).map(_.head)))))

    synchronized {
      promise = Promise[FileUploadRequest]
      result = Future.successful()
    }
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, "/faults/fault1-fault.zip")))(result(promise.future))

    val report2 = DistributionFaultReport("fault2", "client2", FaultInfo(new Date(), "instance2", "directory", "service2", "profile2",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))), None, None, None, None), Seq()), Seq("file2"))
    synchronized {
      promise = Promise[FileUploadRequest]
    }
    result(collections.State_FaultReports.map(_.insert(FaultReportDocument(1, report2))))
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, "/faults/fault2-fault.zip")))(result(promise.future))

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()))
    result(collections.State_UploadStatus.map(_.dropItems()))
  }
}