package distribution.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, Sorts}
import com.vyulabs.update.common.Common.{ClientName, InstanceId, ServiceDirectory, ServiceName}
import com.vyulabs.update.distribution.DistributionDirectory
import com.vyulabs.update.info.{ClientFaultReport, ClientServiceState, InstanceServiceState}
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.DatabaseCollections
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait StateUtils extends GetUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext
  protected implicit val filesLocker: SmartFilesLocker

  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  def setServicesState(clientName: ClientName, instancesState: Seq[InstanceServiceState]): Future[Boolean] = {
    for {
      collection <- collections.ClientsServiceStates
      result <- Future.sequence(instancesState.map(state =>
        collection.insert(ClientServiceState(Some(clientName), state.instanceId, state.serviceName, state.directory, state.state)))).map(_ => true)
    } yield result
  }

  def getServicesState(clientName: Option[ClientName], serviceName: Option[ServiceName],
                       instanceId: Option[InstanceId], directory: Option[ServiceDirectory]): Future[Seq[ClientServiceState]] = {
    val clientArg = clientName.map { client => Filters.eq("clientName", client) }
    val serviceArg = serviceName.map { service => Filters.eq("serviceName", service) }
    val instanceIdArg = instanceId.map { instanceId => Filters.eq("instanceId", instanceId) }
    val directoryArg = directory.map { directory => Filters.eq("directory", directory) }
    val args = clientArg ++ serviceArg ++ instanceIdArg ++ directoryArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.ClientsServiceStates
      profile <- collection.find(filters)
    } yield profile
  }

  def getClientFaultReports(clientName: Option[ClientName], serviceName: Option[ServiceName], last: Option[Int]): Future[Seq[ClientFaultReport]] = {
    val clientArg = clientName.map { client => Filters.eq("clientName", client) }
    val serviceArg = serviceName.map { service => Filters.eq("serviceName", service) }
    val args = clientArg ++ serviceArg
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    // https://stackoverflow.com/questions/4421207/how-to-get-the-last-n-records-in-mongodb
    val sort = last.map { last => Sorts.descending("_id") }
    for {
      collection <- collections.ClientsFaultReports
      faults <- collection.find(filters, sort, last)
    } yield faults
  }
}
