package distribution.mongo

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.client.model.Projections
import com.mongodb.client.result.DeleteResult
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection, Success}
import org.bson.Document
import org.bson.conversions.Bson
import org.slf4j.LoggerFactory
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class MongoDb(dbName: String, connectionString: String = "mongodb://localhost:27017")
             (implicit executionContext: ExecutionContext) {
  implicit val system = ActorSystem(s"Mongo DB: ${dbName}")
  implicit val materializer = ActorMaterializer()

  private val client = MongoClients.create(connectionString)
  private val db = client.getDatabase(dbName)

  def createCollection(name: String): Unit = {
    db.createCollection(name)
  }

  def getCollection[T](name: String): MongoDbCollection[T] = {
    new MongoDbCollection[T](name, db.getCollection(name))
  }
}

class MongoDbCollection[T](name: String, collection: MongoCollection[Document])
                          (implicit materializer: ActorMaterializer, executionContext: ExecutionContext) {
  implicit val log = LoggerFactory.getLogger(getClass)

  def insert(obj: T)(implicit writer: JsonWriter[T]): Future[Boolean] = {
    val version = obj.toJson.compactPrint
    Source.fromPublisher(collection.insertOne(Document.parse(version)))
      .log(s"Insert to Mongo DB collection ${name}")
      .runWith(Sink.headOption[Success]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }

  def find(filters: Bson, sort: Option[Bson] = None, limit: Option[Int])(implicit reader: JsonReader[T]): Future[Seq[T]] = {
    var find = collection.find(filters)
    sort.foreach(sort => find = find.sort(sort))
    limit.foreach(limit => find = find.limit(limit))
    Source.fromPublisher(find).map(_.toJson.parseJson.convertTo[T])
      .log(s"Find in Mongo DB collection ${name}")
      .runWith(Sink.fold[Seq[T], T](Seq.empty[T])((seq, obj) => {seq :+ obj}))
  }

  def delete(filters: Bson): Future[Boolean] = {
    Source.fromPublisher(collection.deleteOne(filters))
      .log(s"Delete from Mongo DB collection ${name}")
      .runWith(Sink.headOption[DeleteResult]).map(_ => true)
      .recover {
        case ex: Exception => false
      }
  }
}