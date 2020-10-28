package distribution

import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.info._
import distribution.mongo.MongoDb
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class DatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) {
  private implicit val log = LoggerFactory.getLogger(this.getClass)

  val VersionInfo = db.getOrCreateCollection[VersionInfo]()
  val DesiredVersions = db.getOrCreateCollection[DesiredVersions]()

  (for {
    versionInfoIndexes <- VersionInfo.map(
      _.createIndex(Indexes.ascending("serviceName", "clientName", "version"), new IndexOptions().unique(true)))
  } yield versionInfoIndexes)
    .foreach(_ => log.info("Common collections are ready"))
}
