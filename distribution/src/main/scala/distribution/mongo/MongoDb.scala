package distribution.mongo

import java.io.IOException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.mongodb.client.result.DeleteResult
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection, Success}
import org.bson.{BsonDocument, Document}
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import spray.json._

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

  def getOrCreateCollection[T](suffix: Option[String] = None)(implicit classTag: ClassTag[T]): Future[MongoDbCollection[T]] = {
    val name = suffix match {
      case Some(suffix) =>
        classTag.runtimeClass.getSimpleName + "-" + suffix
      case None =>
        classTag.runtimeClass.getSimpleName
    }
    for {
      exists <- getCollectionNames().map(_.contains(name))
      collection <- {
        (if (!exists) {
          Source.fromPublisher(db.createCollection(name))
            .log(s"Create Mongo DB collection ${name}")
            .runWith(Sink.headOption[Success])
        } else {
          Future()
        }).map(_ => new MongoDbCollection[T](db.getCollection(name)))
      }
    } yield collection
  }

  def dropDatabase(): Future[Boolean] = {
    Source.fromPublisher(db.drop())
      .log(s"Drop Mongo DB database ${db.getName}")
      .runWith(Sink.headOption[Success]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }
}

class MongoDbCollection[T](collection: MongoCollection[Document])
                          (implicit materializer: ActorMaterializer, executionContext: ExecutionContext, classTag: ClassTag[T]) {
  implicit val log = LoggerFactory.getLogger(getClass)

  val name = classTag.runtimeClass.getSimpleName

  def insert(obj: T)(implicit writer: JsonWriter[T]): Future[Boolean] = {
    val version = obj.toJson.compactPrint
    Source.fromPublisher(collection.insertOne(Document.parse(version)))
      .log(s"Insert to Mongo DB collection ${name}")
      .runWith(Sink.headOption[Success]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }

  def find(filters: Bson, sort: Option[Bson] = None, limit: Option[Int] = None)(implicit reader: JsonReader[T]): Future[Seq[T]] = {
    var find = collection.find(filters)
    sort.foreach(sort => find = find.sort(sort))
    limit.foreach(limit => find = find.limit(limit))
    Source.fromPublisher(find).map(_.toJson.parseJson.convertTo[T])
      .log(s"Find in Mongo DB collection ${name}")
      .runWith(Sink.fold[Seq[T], T](Seq.empty[T])((seq, obj) => {seq :+ obj}))
  }

  def delete(filters: Bson): Future[Boolean] = {
    Source.fromPublisher(collection.deleteMany(filters))
      .log(s"Delete from Mongo DB collection ${name}")
      .runWith(Sink.headOption[DeleteResult]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }

  def dropItems(): Future[Boolean] = {
    Source.fromPublisher(collection.deleteMany(new BsonDocument()))
      .log(s"Drop Mongo DB collection ${name}")
      .runWith(Sink.headOption[DeleteResult]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }

  def drop(): Future[Boolean] = {
    Source.fromPublisher(collection.drop())
      .log(s"Drop Mongo DB collection ${name}")
      .runWith(Sink.headOption[Success]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }
}