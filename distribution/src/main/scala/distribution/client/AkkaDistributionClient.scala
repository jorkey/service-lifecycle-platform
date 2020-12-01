package distribution.client

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.Common.{DistributionName, FaultId, ServiceName}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.GraphqlRequest
import com.vyulabs.update.version.DeveloperDistributionVersion
import org.slf4j.LoggerFactory
import spray.json.JsonReader

import scala.concurrent.{ExecutionContext, Future}

class AkkaDistributionClient(distributionName: DistributionName, client: HttpAkkaClient)
                            (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response] = {
    client.graphqlRequest(request)
  }

  def downloadDeveloperVersionImage(serviceName: ServiceName, version: DeveloperDistributionVersion, file: File): Future[Unit] = {
    client.download(client.makeUrl(loadPathPrefix, developerVersionImagePath, serviceName, version.toString), file)
  }

  def uploadFaultReport(faultId: FaultId, file: File): Future[Unit] = {
    client.upload(client.makeUrl(loadPathPrefix, faultReportPath, faultId), faultReportField, file)
  }
}
