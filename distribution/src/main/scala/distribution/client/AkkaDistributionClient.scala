package distribution.client

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.vyulabs.update.common.Common.{DistributionName, FaultId}
import com.vyulabs.update.distribution.DistributionWebPaths._
import com.vyulabs.update.distribution.client.GraphqlRequest
import org.slf4j.LoggerFactory
import spray.json.JsonReader

import scala.concurrent.{ExecutionContext, Future}

class AkkaDistributionClient(distributionName: DistributionName, client: HttpAkkaClient)
                            (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext)  {
  implicit val log = LoggerFactory.getLogger(this.getClass)

  def graphqlRequest[Response](request: GraphqlRequest[Response])(implicit reader: JsonReader[Response]): Future[Response] = {
    client.graphqlRequest(request)
  }

  def uploadFaultReport(faultId: FaultId, faultReportFile: File): Future[Unit] = {
    client.upload(client.makeUrl(loadPathPrefix, faultReportPath, faultId), faultReportFile)
  }
}
