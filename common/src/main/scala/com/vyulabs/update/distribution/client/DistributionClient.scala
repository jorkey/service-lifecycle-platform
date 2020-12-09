package com.vyulabs.update.distribution.client

import com.vyulabs.update.common.Common
import com.vyulabs.update.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.LoggerFactory
import spray.json.JsonReader

import java.io.File
import scala.concurrent.{ExecutionContext, Future}

class DistributionClient(val distributionName: DistributionName, client: HttpClient)(implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def available(): Future[Unit] = {
    client.exists(pingPath)
  }

  def getDistributionVersion(): Future[Option[ClientDistributionVersion]] = {
    client.graphql(administratorQueries.getServiceStates(Some(distributionName), Some(Common.DistributionServiceName), None, None))
      .map(_.headOption.map(_.instance.service.version).flatten)
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response]= {
    client.graphql(request)
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Future[Unit] = {
    client.download(developerVersionImagePath + "/" + serviceName + "/" + version.toString, file)
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Future[Unit] = {
    client.download(clientVersionImagePath + "/" + serviceName + "/" + version.toString, file)
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Future[Unit] = {
    client.upload(developerVersionImagePath + "/" + serviceName + "/" + version.toString, versionImageField, file)
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Future[Unit] = {
    client.upload(clientVersionImagePath + "/" + serviceName + "/" + version.toString, versionImageField, file)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Future[Unit] = {
    client.upload(faultReportPath + "/" + faultId, faultReportField, faultReportFile)
  }
}
