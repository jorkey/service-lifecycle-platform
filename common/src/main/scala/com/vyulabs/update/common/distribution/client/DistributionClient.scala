package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.JsonReader
import java.io.File

import scala.concurrent.{ExecutionContext, Future}

class DistributionClient[Source[_]](client: HttpClient[Source])
                        (implicit executionContext: ExecutionContext) {
  def available()(implicit log: Logger): Future[Unit] = {
    client.exists(pingPath)
  }

  def getServiceVersion(distributionName: DistributionName, serviceName: ServiceName)
                       (implicit log: Logger): Future[Option[ClientDistributionVersion]] = {
    client.graphql(administratorQueries.getServiceStates(Some(distributionName), Some(serviceName), None, None))
      .map(_.headOption.map(_.instance.service.version).flatten)
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])
                              (implicit reader: JsonReader[Response], log: Logger): Future[Response]= {
    client.graphql(request)
  }

  def graphqlSubRequest[Response](request: GraphqlRequest[Response])
                                 (implicit reader: JsonReader[Response], log: Logger): Future[Source[Response]]= {
    client.graphqlSub(request)
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File)
                                   (implicit log: Logger): Future[Unit] = {
    client.download(developerVersionImagePath + "/" + serviceName + "/" + version.toString, file)
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File)
                                (implicit log: Logger): Future[Unit] = {
    client.download(clientVersionImagePath + "/" + serviceName + "/" + version.toString, file)
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File)
                                 (implicit log: Logger): Future[Unit] = {
    client.upload(developerVersionImagePath + "/" + serviceName + "/" + version.toString, imageField, file)
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File)
                              (implicit log: Logger): Future[Unit] = {
    client.upload(clientVersionImagePath + "/" + serviceName + "/" + version.toString, imageField, file)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File)
                       (implicit log: Logger): Future[Unit] = {
    client.upload(faultReportPath + "/" + faultId, faultReportField, faultReportFile)
  }
}