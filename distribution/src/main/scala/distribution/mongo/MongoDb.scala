package distribution.mongo

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.client.model.{IndexOptions, ReplaceOptions}
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection, Success}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.{BsonDocument, Document}
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class MongoDb(dbName: String, connectionString: String = "mongodb://localhost:27017")
             (implicit executionContext: ExecutionContext) {
  implicit val system = ActorSystem(s"MongoDB_${dbName}")
  implicit val materializer = ActorMaterializer()

  private val client = MongoClients.create(MongoClientSettings.builder
    .applyConnectionString(new ConnectionString(connectionString)).build)

  private val db = client.getDatabase(dbName)

  def getCollectionNames(): Future[Seq[String]] = {
    Source.fromPublisher(db.listCollectionNames())
      .log(s"Get Mongo DB collection names")
      .runWith(Sink.fold[Seq[String], String](Seq.empty[String])((seq, obj) => {seq :+ obj}))
  }

  def collectionExists[T](suffix: Option[String] = None): Future[Boolean] = {
    getCollectionNames().map(_.contains(getCollectionName(suffix)))
  }

  def getCollection[T](suffix: Option[String] = None)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): MongoDbCollection[T] = {
    new MongoDbCollection[T](db.getCollection(getCollectionName(suffix), classTag.runtimeClass.asInstanceOf[Class[T]]).withCodecRegistry(codecRegistry))
  }

  def createCollection[T](suffix: Option[String] = None)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): Future[MongoDbCollection[T]] = {
    val name = getCollectionName(suffix)
    Source.fromPublisher(db.createCollection(name))
      .log(s"Create Mongo DB collection ${name}")
      .runWith(Sink.head[Success])
      .map(_ => getCollection[T](suffix))
  }

  def getOrCreateCollection[T](suffix: Option[String] = None)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): Future[MongoDbCollection[T]] = {
    for {
      exists <- collectionExists[T](suffix)
      collection <- if (!exists) createCollection[T](suffix) else Future(getCollection[T](suffix))
    } yield collection
  }

  def dropDatabase(): Future[Success] = {
    Source.fromPublisher(db.drop())
      .log(s"Drop Mongo DB database ${db.getName}")
      .runWith(Sink.head[Success])
  }

  private def getCollectionName[T](suffix: Option[String] = None)(implicit classTag: ClassTag[T]): String = {
    suffix match {
      case Some(suffix) =>
        classTag.runtimeClass.getSimpleName + "-" + suffix
      case None =>
        classTag.runtimeClass.getSimpleName
    }
  }
}

class MongoDbCollection[T](collection: MongoCollection[T])
                          (implicit materializer: ActorMaterializer, executionContext: ExecutionContext, classTag: ClassTag[T]) {
  implicit val log = LoggerFactory.getLogger(getClass)

  private val name = classTag.runtimeClass.getSimpleName

  def insert(obj: T): Future[Success] = {
    Source.fromPublisher(collection.insertOne(obj))
      .log(s"Insert to Mongo DB collection ${name}")
      .runWith(Sink.head[Success])
  }

  def replace(filters: Bson, obj: T): Future[UpdateResult] = {
    Source.fromPublisher(collection.replaceOne(filters, obj, new ReplaceOptions().upsert(true)))
      .log(s"Replace document in Mongo DB collection ${name}")
      .runWith(Sink.head[UpdateResult])
  }

  def find(filters: Bson = new BsonDocument(), sort: Option[Bson] = None, limit: Option[Int] = None): Future[Seq[T]] = {
    var find = collection.find(filters)
    sort.foreach(sort => find = find.sort(sort))
    limit.foreach(limit => find = find.limit(limit))
    Source.fromPublisher(find)
      .log(s"Find in Mongo DB collection ${name} with filters ${filters}")
      .runWith(Sink.fold[Seq[T], T](Seq.empty[T])((seq, obj) => {seq :+ obj}))
  }

  def updateOne(filters: Bson, update: Bson): Future[UpdateResult] = {
    Source.fromPublisher(collection.updateOne(filters, update))
      .log(s"Update Mongo DB collection ${name} with filters ${filters}")
      .runWith(Sink.head[UpdateResult])
  }

  def createIndex(index: Bson, options: IndexOptions = new IndexOptions()): Future[String] = {
    Source.fromPublisher(collection.createIndex(index, options))
      .log(s"Create index ${index} in Mongo DB collection ${name}")
      .runWith(Sink.head[String])
  }

  def listIndexes(): Future[Seq[Document]] = {
    Source.fromPublisher(collection.listIndexes())
      .runWith(Sink.fold[Seq[Document], Document](Seq.empty[Document])((seq, obj) => {seq :+ obj}))
  }

  def delete(filters: Bson): Future[DeleteResult] = {
    Source.fromPublisher(collection.deleteMany(filters))
      .log(s"Delete from Mongo DB collection ${name} with filters ${filters}")
      .runWith(Sink.head[DeleteResult])
  }

  def dropItems(): Future[DeleteResult] = {
    Source.fromPublisher(collection.deleteMany(new BsonDocument()))
      .log(s"Drop Mongo DB collection ${name} items")
      .runWith(Sink.head)
  }

  def drop(): Future[Success] = {
    Source.fromPublisher(collection.drop())
      .log(s"Drop Mongo DB collection ${name}")
      .runWith(Sink.head[Success])
  }
}