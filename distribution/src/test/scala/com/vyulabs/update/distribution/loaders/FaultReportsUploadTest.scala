package com.vyulabs.update.distribution.loaders

import java.io.{File, IOException}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.{FaultReportDocument, UploadStatus, UploadStatusDocument}

import scala.concurrent.{ExecutionContext, Future, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  case class FileUploadRequest(path: String, file: File)

  var promise = Promise[FileUploadRequest]
  var result = Future.successful()
  var l = 0

  val date = new Date()

  def fileUploadRequest(path: String, file: File): Future[Unit] = {
    synchronized {
      log.info(s"Upload request path ${path} file ${file}")
      promise.success(FileUploadRequest(path, file))
      promise = Promise[FileUploadRequest]
      result
    }
  }

  it should "upload fault reports" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 1,
      (_, _) => Future.failed(new IOException("Not expected")), fileUploadRequest)
    uploader.start()

    val report = DistributionFaultReport("fault1", "distribution1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    testAction(() => result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report))).flatten),"/faults/fault1-fault.zip")

    Thread.sleep(100)
    uploader.stop()

    result(collections.State_FaultReports.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader("distribution", collections, distributionDir, 2,
      (_, _) => Future.failed(new IOException("Not expected")), fileUploadRequest)
    result = Future.failed(new IOException("upload error"))
    uploader.start()

    val report1 = DistributionFaultReport("fault1", "distribution1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1"))
    testAction(() => result(collections.State_FaultReports.map(_.insert(FaultReportDocument(0, report1))).flatten),"/faults/fault1-fault.zip")

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.faultReports", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.faultReports")).map(_.head)))))

    testAction(() => synchronized { result = Future.successful() },"/faults/fault1-fault.zip")

    val report2 = DistributionFaultReport("fault2", "client2", FaultInfo(new Date(), "instance2", "directory", "service2", "profile2",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))), None, None, None, None), Seq()), Seq("file2"))
    testAction(() => result(collections.State_FaultReports.map(_.insert(FaultReportDocument(1, report2)))),"/faults/fault2-fault.zip")

    uploader.stop()

    result(collections.State_ServiceStates.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def testAction(action: () => Unit, file: String): Unit = {
    val promise = this.promise
    action()
    assertResult(FileUploadRequest("upload_fault_report", new File(distributionDir.directory, file)))(result(promise.future))
  }
}