package com.vyulabs.update.distribution.mongo

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mongodb.client.model.{Filters, FindOneAndUpdateOptions, ReturnDocument, Updates}
import org.bson.codecs.DecoderContext
import org.bson.codecs.configuration.CodecRegistry
import org.bson.conversions.Bson
import org.bson.{BsonDateTime, BsonDocument, BsonDocumentReader, BsonDocumentWrapper}
import org.mongodb.scala.bson.BsonInt64
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.{ClassTag, classTag}

class SequencedCollection[T: ClassTag](collection: Future[MongoDbCollection[BsonDocument]], sequenceCollection: Future[MongoDbCollection[SequenceDocument]])
                            (implicit system: ActorSystem, materializer: Materializer, executionContext: ExecutionContext, codecRegistry: CodecRegistry) {
  implicit val log = LoggerFactory.getLogger(getClass)

  private var modifyInProcess = Option.empty[Future[Int]]

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
      docs <- findDocuments(filters)
    } yield {
      docs.map(doc => {
        val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
        codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
      })
    }
  }

  def update(filters: Bson, modify: T => T): Future[Int] = {
    def process(): Future[Int] = {
      for {
        collection <- collection
        docs <- findDocuments(filters)
        result <- {
          Future.sequence(docs.map { doc =>
            for {
              sequence <- getNextSequence(collection.getName(), 1)
              _ <- collection.updateOne(Filters.eq("_id", doc.getInt64("_id")),
                Updates.combine(Updates.set("_replacedBy", new BsonInt64(sequence)), Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))))
              result <- {
                val codec = codecRegistry.get(classTag[T].runtimeClass.asInstanceOf[Class[T]])
                val document = codec.decode(new BsonDocumentReader(doc), DecoderContext.builder.build())
                val newDocument = modify(document)
                val newDoc = BsonDocumentWrapper.asBsonDocument(newDocument, codecRegistry)
                newDoc.append("_id", new BsonInt64(sequence))
                newDoc.append("_time", new BsonDateTime(System.currentTimeMillis()))
                collection.insert(newDoc)
              }
              _ <- collection.updateOne(Filters.eq("_id", doc.getInt64("_id")), Updates.unset("_replacedBy"))
            } yield result
          }).map(_.size)
        }
      } yield result
    }

    def queueFuture(): Future[Int] = {
      synchronized {
        val future = modifyInProcess match {
          case Some(currentProcess) if !currentProcess.isCompleted =>
            currentProcess.transformWith(_ => process())
          case _ =>
            process()
        }
        modifyInProcess = Some(future)
        future.andThen {
          case _ =>
            synchronized {
              modifyInProcess match {
                case Some(`future`) =>
                  modifyInProcess = None
                case _ =>
              }
            }
        }
        future
      }
    }

    queueFuture()
  }

  def delete(filters: Bson = new BsonDocument()): Future[Unit] = {
    for {
      collection <- collection
      result <- collection.updateMany(Filters.and(filters,
        Filters.or(Filters.exists("_expireTime", false))),
        Updates.set("_expireTime", new BsonDateTime(System.currentTimeMillis()))).map(_ => ())
    } yield result
  }

  private def findDocuments(filters: Bson = new BsonDocument()): Future[Seq[BsonDocument]] = {
    for {
      collection <- collection
      docs <- collection.find(Filters.and(filters,
        Filters.or(Filters.exists("_expireTime", false), Filters.exists("_replacedBy", true))))
    } yield {
      val notReplaced = docs.filter(!_.containsKey("_replacedBy")).map(_.get("_id").asInt64())
      docs.filter(doc => { !doc.containsKey("_replacedBy") || !notReplaced.contains(doc.get("_replacedBy").asInt64()) })
    }
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
