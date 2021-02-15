package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.LoggerFactory
import spray.json.JsonReader

import java.io.File
import scala.concurrent.{ExecutionContext, Future}

class DistributionClient[Source[_]](val distributionName: DistributionName, client: HttpClient[Source])
                        (implicit executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def available(): Future[Unit] = {
    client.exists(pingPath)
  }

  def getServiceVersion(serviceName: ServiceName): Future[Option[ClientDistributionVersion]] = {
    client.graphql(administratorQueries.getServiceStates(Some(distributionName), Some(serviceName), None, None))
      .map(_.headOption.map(_.instance.service.version).flatten)
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response]= {
    client.graphql(request)
  }

  def graphqlSubRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Source[Response]]= {
    client.graphqlSub(request)
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Future[Unit] = {
    client.download(developerVersionPath + "/" + serviceName + "/" + version.toString, file)
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Future[Unit] = {
    client.download(clientVersionPath + "/" + serviceName + "/" + version.toString, file)
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Future[Unit] = {
    client.upload(developerVersionPath + "/" + serviceName + "/" + version.toString, imageField, file)
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File): Future[Unit] = {
    client.upload(clientVersionPath + "/" + serviceName + "/" + version.toString, imageField, file)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Future[Unit] = {
    client.upload(faultReportPath + "/" + faultId, faultReportField, faultReportFile)
  }
}
