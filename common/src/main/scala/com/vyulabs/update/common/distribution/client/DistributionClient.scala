package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{DistributionId, FaultId, ServiceId}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.AdministratorGraphqlCoder._
import com.vyulabs.update.common.distribution.client.graphql.{GraphqlRequest, LoginCoder, PingCoder}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.JsonReader

import java.io.{File, IOException}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DistributionClient[Source[_]](client: HttpClient[Source])
                        (implicit executionContext: ExecutionContext) {
  private var loginInProcess = Option.empty[Future[Unit]]

  def ping()(implicit log: Logger): Future[Unit] = {
    client.graphql(PingCoder.ping()).map(_ => ())
  }

  def login()(implicit log: Logger): Future[Unit] = {
    synchronized {
      loginInProcess match {
        case None =>
          client.accessToken match {
            case None =>
              val authTokenRx = "(.*):(.*)".r
              client.distributionUrl.getUserInfo match {
                case authTokenRx(user, password) =>
                  val future = client.graphql(LoginCoder.login(user, password))
                    .andThen { case result =>
                      synchronized {
                        loginInProcess = None
                        result match {
                          case Success(token) =>
                            client.accessToken = Some(token)
                          case Failure(_) =>
                        }
                      }
                    }
                    .map(_ => ())
                  loginInProcess = Some(future)
                  future
                case _ =>
                  Future.failed(new IOException("No authentication data in the URL for login"))
              }
            case Some(_) =>
              Future()
          }
        case Some(loginInProcess) =>
          loginInProcess
      }
    }
  }

  def graphqlRequest[Response](request: GraphqlRequest[Response])
                              (implicit reader: JsonReader[Response], log: Logger): Future[Response]= {
    login().map(_ => client.graphql(request)).flatten
  }

  def graphqlSubRequest[Response](request: GraphqlRequest[Response])
                                 (implicit reader: JsonReader[Response], log: Logger): Future[Source[Response]]= {
    login().map(_ => client.graphqlSub(request)).flatten
  }

  def downloadDeveloperVersionImage(service: ServiceId, version: DeveloperDistributionVersion, file: File)
                                   (implicit log: Logger): Future[Unit] = {
    login().map(_ => client.download(developerVersionImagePath + "/" + service + "/" + version.toString, file)).flatten
  }

  def downloadClientVersionImage(service: ServiceId, version: ClientDistributionVersion, file: File)
                                (implicit log: Logger): Future[Unit] = {
    login().map(_ => client.download(clientVersionImagePath + "/" + service + "/" + version.toString, file)).flatten
  }

  def uploadDeveloperVersionImage(service: ServiceId, version: DeveloperDistributionVersion, file: File)
                                 (implicit log: Logger): Future[Unit] = {
    login().map(_ => client.upload(developerVersionImagePath + "/" + service + "/" + version.toString, imageField, file)).flatten
  }

  def uploadClientVersionImage(service: ServiceId, version: ClientDistributionVersion, file: File)
                              (implicit log: Logger): Future[Unit] = {
    login().map(_ => client.upload(clientVersionImagePath + "/" + service + "/" + version.toString, imageField, file)).flatten
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File)
                       (implicit log: Logger): Future[Unit] = {
    login().map(_ => client.upload(faultReportPath + "/" + faultId, faultReportField, faultReportFile)).flatten
  }
}