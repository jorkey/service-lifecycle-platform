package distribution.developer.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.stream.Materializer
import com.mongodb.client.model.Filters
import com.vyulabs.update.common.Common._
import com.vyulabs.update.config.{ClientConfig, ClientInfo, InstallProfile}
import com.vyulabs.update.distribution.developer.{DeveloperDistributionDirectory, DeveloperDistributionWebPaths}
import com.vyulabs.update.lock.SmartFilesLocker
import distribution.mongo.MongoDb
import distribution.utils.{GetUtils, PutUtils}
import org.bson.BsonDocument
import org.slf4j.LoggerFactory
import spray.json._

import scala.collection.JavaConverters.asJavaIterableConverter
import scala.concurrent.{ExecutionContext, Future}

trait ClientsUtils extends GetUtils with PutUtils with DeveloperDistributionWebPaths with SprayJsonSupport {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  protected implicit val system: ActorSystem
  protected implicit val materializer: Materializer
  protected implicit val executionContext: ExecutionContext

  protected val dir: DeveloperDistributionDirectory
  protected val mongoDb: MongoDb

  protected val filesLocker: SmartFilesLocker

  def getClientsInfo(clientName: Option[ClientName] = None): Future[Seq[ClientInfo]] = {
    val clientArg = clientName.map(Filters.eq("clientName", _))
    val args = clientArg.toSeq
    val filters = if (!args.isEmpty) Filters.and(args.asJava) else new BsonDocument()
    for {
      collection <- mongoDb.getOrCreateCollection[ClientInfo]()
      info <- collection.find(filters)
    } yield info
  }

  def getClientConfig(clientName: ClientName): Future[ClientConfig] = {
    getClientsInfo(Some(clientName)).map(_.head.clientConfig)
  }

  def getClientInstallProfile(clientName: ClientName): Future[InstallProfile] = {
    for {
      clientConfig <- getClientConfig(clientName)
      installProfile <- getInstallProfile(clientConfig.installProfile)
    } yield installProfile
  }

  def getInstallProfile(profileName: ProfileName): Future[InstallProfile] = {
    val profileArg = Filters.eq("profileName", profileName)
    for {
      collection <- mongoDb.getOrCreateCollection[InstallProfile]()
      profile <- collection.find(profileArg).map(_.head)
    } yield profile
  }
}
