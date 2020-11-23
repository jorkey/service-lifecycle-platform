package com.vyulabs.update.distribution.loaders

import java.io.{File, IOException}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceFaultReport, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.loaders.StateUploader
import distribution.mongo.{FaultReportDocument, UploadStatus, UploadStatusDocument}
import spray.json.{JsValue, enrichAny}

import scala.concurrent.{ExecutionContext, Future, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  case class GraphqlMutationRequest(command: String, arguments: Map[String, JsValue])
  case class FileUploadRequest(path: String, file: File)

  var uploadPromise = Promise[FileUploadRequest]
  var uploadResult = Future.successful()

  var graphqlPromise = Promise[GraphqlMutationRequest]
  var graphqlResult = Future.successful()

  val date = new Date()

  def graphqlMutationRequest(command: String, arguments: Map[String, JsValue]): Future[Unit] = {
    synchronized {
      log.info(s"Request command ${command} arguments ${arguments}")
      graphqlPromise.success(GraphqlMutationRequest(command, arguments))
      graphqlPromise = Promise[GraphqlMutationRequest]
      graphqlResult
    }
  }

  def fileUploadRequest(path: String, file: File): Future[Unit] = {
    synchronized {
      log.info(s"Upload request path ${path} file ${file}")
      uploadPromise.success(FileUploadRequest(path, file))
      uploadPromise = Promise[FileUploadRequest]
      uploadResult
    }
  }

  it should "upload fault reports" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, 1, graphqlMutationRequest, fileUploadRequest)
    uploader.start()

    val report = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1")))
    testAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(0, report))).flatten), report, "/faults/fault1-fault.zip")

    Thread.sleep(100)
    uploader.stop()

    result(collections.State_FaultReportsInfo.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, 2, graphqlMutationRequest, fileUploadRequest)
    uploadResult = Future.failed(new IOException("upload error"))
    uploader.start()

    val report1 = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1")))
    testUploadAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(0, report1))).flatten), "/faults/fault1-fault.zip")

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.faultReportsInfo", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.faultReportsInfo")).map(_.head)))))

    testAction(() => synchronized { uploadResult = Future.successful() }, report1, "/faults/fault1-fault.zip")

    val report2 = DistributionFaultReport(distributionName, ServiceFaultReport("fault2", FaultInfo(new Date(), "instance2", "directory", "service2", "profile2",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))), None, None, None, None), Seq()), Seq("file2")))
    testAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(1, report2)))), report2,"/faults/fault2-fault.zip")

    uploader.stop()

    result(collections.State_FaultReportsInfo.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def testUploadAction(action: () => Unit, file: String): Unit = {
    val uploadPromise = this.uploadPromise
    action()
    assertResult(FileUploadRequest("uploadFaultReport", new File(distributionDir.directory, file)))(result(uploadPromise.future))
  }

  def testAction(action: () => Unit, report: DistributionFaultReport, file: String): Unit = {
    val uploadPromise = this.uploadPromise
    val graphqlPromise = this.graphqlPromise
    action()
    assertResult(FileUploadRequest("uploadFaultReport", new File(distributionDir.directory, file)))(result(uploadPromise.future))
    val graphqlRequest = result(graphqlPromise.future)
    assertResult("addServiceFaultReportInfo")(graphqlRequest.command)
    assertResult(Map("fault" -> report.report.toJson))(graphqlRequest.arguments)
  }
}