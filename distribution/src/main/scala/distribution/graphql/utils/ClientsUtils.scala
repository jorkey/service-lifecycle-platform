package distribution.graphql.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common.{ClientName, ProfileName}
import com.vyulabs.update.config.{ClientConfig, ClientInfo, ClientProfile}
import com.vyulabs.update.distribution.DistributionDirectory
import distribution.graphql.NotFoundException
import distribution.mongo.{ClientProfileDocument, DatabaseCollections}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientsUtils extends GetUtils with PutUtils with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val dir: DistributionDirectory
  protected val collections: DatabaseCollections

  def getClientsInfo(clientName: Option[ClientName] = None): Future[Seq[ClientInfo]] = {
    val clientArg = clientName.map(Filters.eq("clientName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- collections.Developer_ClientsInfo
      info <- collection.find(filters).map(_.map(_.info))
    } yield info
  }

  def getClientConfig(clientName: ClientName): Future[ClientConfig] = {
    getClientsInfo(Some(clientName)).map(_.headOption.map(_.clientConfig).getOrElse(throw NotFoundException(s"No client ${clientName} config")))
  }

  def getClientInstallProfile(clientName: ClientName): Future[ClientProfileDocument] = {
    for {
      clientConfig <- getClientConfig(clientName)
      installProfile <- getInstallProfile(clientConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName): Future[ClientProfileDocument] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- collections.Developer_ClientsProfiles
      profile <- collection.find(profileArg).map(_.headOption
        .getOrElse(throw NotFoundException(s"No install profile ${profileName}")))
    } yield profile
  }
}
