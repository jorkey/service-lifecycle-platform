package com.vyulabs.update.distribution.loaders

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.common.Common.FaultId
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.DistributionClient
import com.vyulabs.update.common.distribution.client.graphql.GraphqlArgument
import com.vyulabs.update.common.info.{DistributionFaultReport, FaultInfo, ServiceFaultReport, ServiceState}
import com.vyulabs.update.common.version.ClientDistributionVersion
import com.vyulabs.update.distribution.TestEnvironment
import com.vyulabs.update.distribution.client.AkkaHttpClient.AkkaSource
import com.vyulabs.update.distribution.client.HttpClientTestStub
import com.vyulabs.update.distribution.mongo.{UploadStatus, UploadStatusDocument}
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Promise}

class FaultReportsUploadTest extends TestEnvironment {
  behavior of "Fault Reports Upload"

  implicit val system = ActorSystem("Distribution")
  implicit val materializer: Materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(null, ex => { ex.printStackTrace(); log.error("Uncatched exception", ex) })

  val date = new Date()

  val httpClient = new HttpClientTestStub[AkkaSource]()
  val distributionClient = new DistributionClient(httpClient)

  distributionClient.login()
  waitForLogin().success("token123")

  it should "upload fault reports" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, FiniteDuration(1, TimeUnit.SECONDS), distributionClient)
    uploader.start()

    val report = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "service1", "directory", "profile1", ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1), 0)), None, None, None, None), Seq()), Seq("file1")))
    result(collections.State_FaultReportsInfo.insert(report))
    waitForFaultReportUpload("fault1").success()
    waitForAddServiceFaultReportInfo(report).success(true)

    Thread.sleep(500)
    uploader.stop()

    result(collections.State_FaultReportsInfo.drop())
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
 }

  it should "try to upload service states again after failure" in {
    val uploader = new StateUploader(distributionName, collections, distributionDir, FiniteDuration(2, TimeUnit.SECONDS), distributionClient)
    uploader.start()

    val report1 = DistributionFaultReport(distributionName, ServiceFaultReport("fault1", FaultInfo(new Date(), "instance1", "service1", "directory", "profile1", ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(1), 0)), None, None, None, None), Seq()), Seq("file1")))
    result(collections.State_FaultReportsInfo.insert(report1))
    waitForFaultReportUpload( "fault1").failure(new IOException("upload error"))

    Thread.sleep(500)
    assertResult(UploadStatusDocument("state.faultReports", UploadStatus(None, Some("upload error"))))(
      result(result(collections.State_UploadStatus.map(_.find(Filters.eq("component", "state.faultReports")).map(_.head)))))

    waitForFaultReportUpload( "fault1").success()
    waitForAddServiceFaultReportInfo(report1).success(true)

    val report2 = DistributionFaultReport(distributionName, ServiceFaultReport("fault2", FaultInfo(new Date(), "instance2", "service2", "directory", "profile2", ServiceState(new Date(), None, None, version = Some(ClientDistributionVersion("test", Seq(2), 0)), None, None, None, None), Seq()), Seq("file2")))
    result(collections.State_FaultReportsInfo.insert(report2))
    waitForFaultReportUpload( "fault2").success()
    waitForAddServiceFaultReportInfo(report2).success(true)

    uploader.stop()

    result(collections.State_FaultReportsInfo.drop())
    result(collections.State_UploadStatus.map(_.dropItems()).flatten)
  }

  def waitForLogin(): Promise[String] = {
    httpClient.waitForMutation("login", Seq(GraphqlArgument("account" -> "test"), GraphqlArgument("password" -> "test")))
  }

  def waitForFaultReportUpload(faultId: FaultId): Promise[Unit] = {
    httpClient.waitForUpload(faultReportPath + "/" + faultId, "fault-report")
  }

  def waitForAddServiceFaultReportInfo(report: DistributionFaultReport): Promise[Boolean] = {
    httpClient.waitForMutation("addServiceFaultReportInfo", Seq(GraphqlArgument("fault" -> report.report.toJson)))
  }
}