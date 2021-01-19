package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

class SequencedCollection[T: ClassTag](collection: Future[MongoDbCollection[BsonDocument]], sequenceCollection: Future[MongoDbCollection[SequenceDocument]])
                            (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext, codecRegistry: CodecRegistry) {
  def insert(document: T): Future[Unit] = {
    insert(Seq(document))
  }

  def insert(documents: Seq[T]): Future[Unit] = {
    for {
      collection <- collection
      sequence <- getNextSequence(collection.getName(), documents.size)
      result <- {
        var seq = sequence - documents.size + 1
        val docs = documents.map { document =>
          val doc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
          doc.append("_id", new BsonInt64(seq)); seq += 1
          doc.append("_time", new BsonDateTime(System.currentTimeMillis()))
          doc
        }
        collection.insert(docs).map(_ => ())
      }
    } yield result
  }

  def find(filters: Bson = new BsonDocument()): Future[Seq[T]] = {
    for {
      collection <- collection
      docs <- collection.find(Filters.and(filters, Filters.exists("_expireTime", false)))
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
      })
    }
  }

  def update(filters: Bson, modify: T => T): Future[Int] = {
    for {
      collection <- collection
      documents <- find(filters)
      result <- collection.updateMany(Filters.and(filters, Filters.exists("_expireTime", false)),
        Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))).map(_ => ())
      result <- {
        insert(documents.map(modify(_))).map(_ => documents.size)
      }
    } yield result
  }

  def delete(filters: Bson): Future[Unit] = {
    for {
      collection <- collection
      result <- collection.updateMany(filters,
        Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))).map(_ => ())
    } yield result
  }

  private def getNextSequence(sequenceName: String, increment: Int = 1): Future[Long] = {
    (for {
      sequenceCollection <- sequenceCollection
      sequence <- { sequenceCollection.findOneAndUpdate(
        Filters.eq("name", sequenceName), Updates.inc("sequence", increment),
        new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)) }
    } yield sequence).map(_.map(_.sequence).head)
  }
}
