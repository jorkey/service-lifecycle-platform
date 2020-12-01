package com.vyulabs.update.distribution.loaders

import java.io.{File, IOException}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.FaultId
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.info.{DistributionFaultReport, FaultInfo, ServiceFaultReport, ServiceState}
import com.vyulabs.update.version.{ClientDistributionVersion, ClientVersion, DeveloperVersion}
import distribution.client.{AsyncDistributionClient, AsyncHttpClient}
import distribution.loaders.StateUploader
import distribution.mongo.{FaultReportDocument, UploadStatus, UploadStatusDocument}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.graphql.{GraphqlArgument, GraphqlMutation, GraphqlRequest}

import scala.concurrent.{ExecutionContext, Future, Promise}
import spray.json._
import spray.json.DefaultJsonProtocol._

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  case class FileUploadRequest(path: String, fieldName: String, file: File)

  var uploadPromise = Promise[FileUploadRequest]
  var uploadResult = Future.successful()

  var graphqlPromise = Promise[GraphqlRequest[Boolean]]
  var graphqlResult = Future.successful[Boolean](true)

  val date = new Date()

  val httpClient = new AsyncHttpClient {
    override def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response] = {
      synchronized {
        log.info(s"Request ${request}")
        graphqlPromise.success(request.asInstanceOf[GraphqlRequest[Boolean]])
        graphqlPromise = Promise[GraphqlRequest[Boolean]]
        graphqlResult.asInstanceOf[Future[Response]]
      }
    }

    override def upload(path: String, fieldName: String, file: File): Future[Unit] = {
      synchronized {
        log.info(s"Upload request path ${path} file ${file}")
        uploadPromise.success(FileUploadRequest(path, fieldName, file))
        uploadPromise = Promise[FileUploadRequest]
        uploadResult
      }
    }

    override def download(path: String, file: File): Future[Unit] = {
      throw new NotImplementedError()
    }
  }

  val distributionClient = new AsyncDistributionClient(httpClient)

  it should "upload fault reports" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, 1, distributionClient)
    uploader.start()

    val report = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1")))
    testAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(0, report))).flatten), report, "fault1", "/faults/fault1-fault.zip")

    Thread.sleep(100)
    uploader.stop()

    result(collections.State_FaultReportsInfo.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, 2, distributionClient)
    uploadResult = Future.failed(new IOException("upload error"))
    uploader.start()

    val report1 = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "directory", "service1", "profile1",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(1))))), None, None, None, None), Seq()), Seq("file1")))
    testUploadAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(0, report1))).flatten), "fault1", "/faults/fault1-fault.zip")

    Thread.sleep(100)
    assertResult(UploadStatusDocument("state.faultReportsInfo", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.faultReportsInfo")).map(_.head)))))

    testAction(() => synchronized { uploadResult = Future.successful() }, report1, "fault1", "/faults/fault1-fault.zip")

    val report2 = DistributionFaultReport(distributionName, ServiceFaultReport("fault2", FaultInfo(new Date(), "instance2", "directory", "service2", "profile2",
      ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", ClientVersion(DeveloperVersion(Seq(2))))), None, None, None, None), Seq()), Seq("file2")))
    testAction(() => result(collections.State_FaultReportsInfo.map(_.insert(FaultReportDocument(1, report2)))), report2, "fault2", "/faults/fault2-fault.zip")

    uploader.stop()

    result(collections.State_FaultReportsInfo.map(_.dropItems()).flatten)
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def testUploadAction(action: () => Unit, faultId: FaultId, file: String): Unit = {
    val uploadPromise = this.uploadPromise
    action()
    assertResult(FileUploadRequest(faultReportPath + "/" + faultId, "fault-report",
      new File(distributionDir.directory, file)))(result(uploadPromise.future))
  }

  def testAction(action: () => Unit, report: DistributionFaultReport, faultId: String, file: String): Unit = {
    val uploadPromise = this.uploadPromise
    val graphqlPromise = this.graphqlPromise
    action()
    assertResult(FileUploadRequest(faultReportPath + "/" + faultId, "fault-report",
      new File(distributionDir.directory, file)))(result(uploadPromise.future))
    val graphqlRequest = result(graphqlPromise.future)
    assertResult(GraphqlMutation("addServiceFaultReportInfo", Seq(GraphqlArgument("fault" -> report.report.toJson))))(graphqlRequest)
  }
}