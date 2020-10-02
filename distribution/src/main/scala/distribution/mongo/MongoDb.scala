package distribution.mongo

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.mongodb.client.result.DeleteResult
import com.mongodb.reactivestreams.client.{MongoClients, MongoCollection, Success}
import org.bson.Document
import org.bson.conversions.Bson
import spray.json._

import scala.concurrent.Future

class MongoDb(dbName: String, connectionString: String = "mongodb://localhost:27017") {
  implicit val system = ActorSystem(s"Mongo DB: ${dbName}")
  implicit val materializer = ActorMaterializer()

  private val client = MongoClients.create(connectionString)
  private val db = client.getDatabase(dbName)

  def createCollection(name: String): Unit = {
    db.createCollection(name)
  }

  def getCollection[T](name: String): MongoDbCollection[T] = {
    new MongoDbCollection[T](db.getCollection(name))
  }
}

class MongoDbCollection[T](collection: MongoCollection[Document])
                          (implicit materializer: ActorMaterializer) {
  def insert(obj: T)(implicit writer: JsonWriter[T]): Source[Success, NotUsed] = {
    val version = obj.toJson.compactPrint
    Source.fromPublisher(collection.insertOne(Document.parse(version)))
  }

  def find(filter: Bson)(implicit reader: JsonReader[T]): Future[Seq[T]] = {
    Source.fromPublisher(collection.find(filter)).map(_.toJson.parseJson.convertTo[T])
      .runWith(Sink.fold[Seq[T], T](Seq.empty[T])((seq, obj) => {seq :+ obj}))
  }

  def delete(filter: Bson): Source[DeleteResult, NotUsed] = {
    Source.fromPublisher(collection.deleteOne(filter))
  }
}