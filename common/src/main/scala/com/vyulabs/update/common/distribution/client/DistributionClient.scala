package com.vyulabs.update.common.distribution.client

import com.vyulabs.update.common.common.Common.{FaultId, ServiceId}
import com.vyulabs.update.common.distribution.DistributionWebPaths._
import com.vyulabs.update.common.distribution.client.graphql.{GraphqlRequest, LoginCoder, PingCoder}
import com.vyulabs.update.common.version.{ClientDistributionVersion, DeveloperDistributionVersion}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import spray.json.JsonReader

import java.io.{File, IOException}
import java.net.{URL, URLConnection, URLStreamHandler}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


class DistributionClient[Source[_]](client: HttpClient[Source])
                        (implicit executionContext: ExecutionContext) {
  private var loginInProcess = Option.empty[Future[Unit]]

  val url = client.distributionUrl

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
              new URL(null, client.distributionUrl, new URLStreamHandler() {
                override def openConnection(u: URL): URLConnection = null
              }
              ).getUserInfo match {
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
    login().flatMap(_ => client.graphql(request))
  }

  def graphqlRequestSSE[Response](request: GraphqlRequest[Response])
                                 (implicit reader: JsonReader[Response], log: Logger): Future[Source[Response]]= {
    login().flatMap(_ => client.subscribeSSE(request))
  }

  def graphqlRequestWS[Response](request: GraphqlRequest[Response])
                                (implicit reader: JsonReader[Response], log: Logger): Future[Source[Response]]= {
    login().flatMap(_ => client.subscribeWS(request))
  }

  def downloadDeveloperVersionImage(service: ServiceId, version: DeveloperDistributionVersion, file: File)
                                   (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.download(developerVersionImagePath + "/" + service + "/" + version.toString, file))
  }

  def downloadClientVersionImage(service: ServiceId, version: ClientDistributionVersion, file: File)
                                (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.download(clientVersionImagePath + "/" + service + "/" + version.toString, file))
  }

  def downloadDeveloperPrivateFile(path: String, file: File)
                                  (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.download(developerPrivateFilePath + "/" + encode(path), file))
  }

  def downloadClientPrivateFile(path: String, file: File)
                               (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.download(clientPrivateFilePath + "/" + encode(path), file))
  }

  def uploadDeveloperVersionImage(service: ServiceId, version: DeveloperDistributionVersion, file: File)
                                 (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.upload(developerVersionImagePath + "/" + service + "/" + version.toString, imageField, file))
  }

  def uploadClientVersionImage(service: ServiceId, version: ClientDistributionVersion, file: File)
                              (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.upload(clientVersionImagePath + "/" + service + "/" + version.toString, imageField, file))
  }

  def uploadFaultReport(id: FaultId, faultReportFile: File)
                       (implicit log: Logger): Future[Unit] = {
    login().flatMap(_ => client.upload(faultReportPath + "/" + id, faultReportField, faultReportFile))
  }
}