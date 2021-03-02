package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.client.model.{FindOneAndUpdateOptions, IndexOptions, ReplaceOptions, UpdateOptions}
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection, Success}
import com.mongodb.{ConnectionString, MongoClientSettings}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDocument, Document}

import java.util.concurrent.TimeUnit
import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

class MongoDb(dbName: String, connectionString: String = "mongodb://localhost:27017", temporary: Boolean = false)
             (implicit executionContext: ExecutionContext) {
  implicit val system = ActorSystem(s"MongoDB_${dbName}")
  implicit val materializer = ActorMaterializer()
  implicit val log = Logging(system, this.getClass)

  private val client = MongoClients.create(MongoClientSettings.builder
    .applyConnectionString(new ConnectionString(connectionString))
    //.applyToConnectionPoolSettings(builder => builder.maxSize(10).minSize(10))
    .build)

  private val db = client.getDatabase(dbName)

  if (temporary) {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        log.info(s"Shutdown: drop temporary database '${dbName}'")
        Await.result(dropDatabase(), FiniteDuration(3, TimeUnit.SECONDS))
      }})
  }

  def getCollectionNames(): Future[Seq[String]] = {
    Source.fromPublisher(db.listCollectionNames())
      .log(s"Get Mongo DB collection names")
      .runWith(Sink.fold[Seq[String], String](Seq.empty[String])((seq, obj) => {seq :+ obj}))
  }

  def collectionExists[T](name: String): Future[Boolean] = {
    getCollectionNames().map(_.contains(name))
  }

  def getCollection[T](name: String)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): MongoDbCollection[T] = {
    new MongoDbCollection[T](name, db.getCollection(name, classTag.runtimeClass.asInstanceOf[Class[T]]).withCodecRegistry(codecRegistry))
  }

  def createCollection[T](name: String)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): Future[MongoDbCollection[T]] = {
    Source.fromPublisher(db.createCollection(name))
      .log(s"Create Mongo DB collection ${name}")
      .runWith(Sink.head[Success])
      .map(_ => getCollection[T](name))
  }

  def getOrCreateCollection[T](name: String)(implicit classTag: ClassTag[T], codecRegistry: CodecRegistry): Future[MongoDbCollection[T]] = {
    for {
      exists <- collectionExists[T](name)
      collection <- if (!exists) createCollection[T](name) else Future(getCollection[T](name))
    } yield collection
  }

  def dropDatabase(): Future[Success] = {
    Source.fromPublisher(db.drop())
      .log(s"Drop Mongo DB database ${db.getName}")
      .runWith(Sink.head[Success])
  }
}

class MongoDbCollection[T](name: String, collection: MongoCollection[T])
                          (implicit system: ActorSystem, materializer: ActorMaterializer, executionContext: ExecutionContext, classTag: ClassTag[T]) {
  implicit val log = Logging(system, this.getClass)

  def getName() = name

  def insert(obj: T): Future[Success] = {
    Source.fromPublisher(collection.insertOne(obj))
      .log(s"Insert to Mongo DB collection ${name}")
      .runWith(Sink.head[Success])
  }

  def insert(objs: Seq[T]): Future[Success] = {
    Source.fromPublisher(collection.insertMany(objs.toList.asJava))
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

  def findOneAndUpdate(filters: Bson, update: Bson, options: FindOneAndUpdateOptions = new FindOneAndUpdateOptions().upsert(true)): Future[Seq[T]] = {
    Source.fromPublisher(collection.findOneAndUpdate(filters, update, options))
      .log(s"Find and update Mongo DB collection ${name} with filters ${filters}, update ${update}")
      .runWith(Sink.fold[Seq[T], T](Seq.empty[T])((seq, obj) => {seq :+ obj}))
  }

  def updateOne(filters: Bson, update: Bson, options: UpdateOptions = new UpdateOptions().upsert(true)): Future[UpdateResult] = {
    Source.fromPublisher(collection.updateOne(filters, update, options))
      .log(s"Update Mongo DB collection ${name} with filters ${filters}")
      .runWith(Sink.head[UpdateResult])
  }

  def updateMany(filters: Bson, update: Bson, options: UpdateOptions = new UpdateOptions().upsert(true)): Future[UpdateResult] = {
    Source.fromPublisher(collection.updateMany(filters, update, options))
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

  def delete(filters: Bson = new BsonDocument()): Future[DeleteResult] = {
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
