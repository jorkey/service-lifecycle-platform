package distribution

import com.mongodb.client.model.{IndexOptions, Indexes}
import com.vyulabs.update.info._
import distribution.mongo.MongoDb

import scala.concurrent.ExecutionContext

class DatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) {
  val VersionInfo = db.getOrCreateCollection[VersionInfo]()
  val DesiredVersions = db.getOrCreateCollection[DesiredVersions]()

  VersionInfo.foreach { collection =>
    collection.listIndexes().foreach { indexes =>
      if (indexes.isEmpty) {
        collection.createIndex(Indexes.ascending("serviceName", "clientName"))
        collection.createIndex(Indexes.ascending("version"), new IndexOptions().unique(true))
      }
    }
  }
}
