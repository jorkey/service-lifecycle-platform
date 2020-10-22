package distribution.client

import distribution.DatabaseCollections
import distribution.mongo.MongoDb

import scala.concurrent.ExecutionContext

class ClientDatabaseCollections(db: MongoDb)(implicit executionContext: ExecutionContext) extends DatabaseCollections(db) {
}
