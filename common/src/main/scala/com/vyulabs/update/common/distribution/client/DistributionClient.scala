package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.GraphqlRequest
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.JsonReader

import java.io.{File, IOException}
import scala.concurrent.{ExecutionContext, Future}

class DistributionClient[Source[_]](client: HttpClient[Source])
                        (implicit executionContext: ExecutionContext) {
  def available()(implicit log: Logger): Future[Unit] = {
    client.exists(pingPath)
  }

  def getServiceVersion(distributionName: DistributionName, serviceName: ServiceName)
                       (implicit log: Logger): Future[Option[ClientDistributionVersion]] = {
    maybeLogin().map(_ => client.graphql(administratorQueries.getServiceStates(Some(distributionName), Some(serviceName), None, None))
      .map(_.headOption.map(_.instance.service.version).flatten)).flatten
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])
                              (implicit reader: JsonReader[Response], log: Logger): Future[Response]= {
    maybeLogin().map(_ => client.graphql(request)).flatten
  }

  def graphqlSubRequest[Response](request: GraphqlRequest[Response])
                                 (implicit reader: JsonReader[Response], log: Logger): Future[Source[Response]]= {
    maybeLogin().map(_ => client.graphqlSub(request)).flatten
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File)
                                   (implicit log: Logger): Future[Unit] = {
    maybeLogin().map(_ => client.download(developerVersionImagePath + "/" + serviceName + "/" + version.toString, file)).flatten
  }

  def downloadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File)
                                (implicit log: Logger): Future[Unit] = {
    maybeLogin().map(_ => client.download(clientVersionImagePath + "/" + serviceName + "/" + version.toString, file)).flatten
  }

  def uploadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File)
                                 (implicit log: Logger): Future[Unit] = {
    maybeLogin().map(_ => client.upload(developerVersionImagePath + "/" + serviceName + "/" + version.toString, imageField, file)).flatten
  }

  def uploadClientVersionImage(serviceName: ServiceName, version: ClientDistributionVersion, file: File)
                              (implicit log: Logger): Future[Unit] = {
    maybeLogin().map(_ => client.upload(clientVersionImagePath + "/" + serviceName + "/" + version.toString, imageField, file)).flatten
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File)
                       (implicit log: Logger): Future[Unit] = {
    maybeLogin().map(_ => client.upload(faultReportPath + "/" + faultId, faultReportField, faultReportFile)).flatten
  }

  private def maybeLogin()(implicit log: Logger): Future[Unit] = {
    client.accessToken match {
      case None =>
        val authTokenRx = "(.*):(.*)".r
        client.distributionUrl.getUserInfo match {
          case authTokenRx(userName, password) =>
            client.graphql(administratorMutations.login(userName, password))
              .map(token => client.accessToken = Some(token))
          case _ =>
            Future.failed(new IOException("No authentication data in the URL for login"))
        }
      case Some(_) =>
        Future()
    }
  }
}