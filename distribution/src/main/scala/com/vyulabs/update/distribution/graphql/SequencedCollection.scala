package com.vyulabs.update.distribution.graphql

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import com.mongodb.client.result.UpdateResult
import com.vyulabs.update.distribution.mongo.{MongoDbCollection, SequenceDocument}
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64

import java.io.IOException
import scala.concurrent.{ExecutionContext, Future}

class SequencedCollection[T](collection: MongoDbCollection[BsonDocument], sequenceCollection: Future[MongoDbCollection[SequenceDocument]])
                            (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext) {
  def insertSequencedDocuments(documents: Seq[T])
                              (implicit codecRegistry: CodecRegistry): Future[Unit] = {
    for {
      sequence <- getNextSequence(collection.getName(), documents.size)
      result <- {
        val docs = documents.map { document =>
          val doc = BsonDocumentWrapper.asBsonDocument(document, codecRegistry)
          doc.append("_id", new BsonInt64(sequence /* - TODO */))
          doc.append("_time", new BsonDateTime(System.currentTimeMillis()))
          doc
        }
        collection.insert(docs).map(_ => ())
      }
    } yield result
  }

  def archiveSequencedDocuments(filters: Bson): Future[Unit] = {
    for {
      result <- collection.updateMany(Filters.and(filters, Filters.exists("_expireTime", false)),
        Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))).map(_ => ())
    } yield result
  }

  def updateOne(collection: MongoDbCollection[BsonDocument], filters: Bson, update: Bson): Future[UpdateResult] = {
    for {
      docs <- collection.find(Filters.and(filters, Filters.exists("_expireTime", false)))
      sequence <- getNextSequence(collection.getName(), 1)
      result <- {
        if (docs.size != 1) {
          throw new IOException("...") // TODO
        }
        val doc = docs.head
        collection.updateMany(Filters.eq("_id", doc.getInt64("_id")),
          Updates.combine(Updates.set("_replacedBy", new BsonInt64(sequence)), Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))))
      }
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
